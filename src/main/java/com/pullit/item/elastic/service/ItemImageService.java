package com.pullit.item.elastic.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullit.item.config.DifficultyDistribution;
import com.pullit.item.dto.request.SmartSelectionRequest;
import com.pullit.item.dto.response.ItemSearchResponse;
import com.pullit.item.dto.response.SmartSelectionResponse;
import com.pullit.item.elastic.document.ItemImageDocument;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemImageService {

    private static final String INDEX_NAME = "item_image";
    private static final int MAX_SEARCH_SIZE = 10000;
    private final Random random = new Random();
    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // --- public entry point -------------------------------------------------
    public SmartSelectionResponse smartSelectItems(SmartSelectionRequest request) throws IOException {
        log.info("스마트 문항 선택 시작 - subjectId: {}, itemCount: {}, difficulty: {}, includePassage: {}",
                request.getSubjectId(), request.getItemCount(), request.getDifficulty(), request.isIncludePassage());

        DifficultyDistribution difficultyDist = DifficultyDistribution.fromCode(request.getDifficulty());
        Map<Long, Integer> targetCounts = calculateTargetCounts(difficultyDist, request.getItemCount());

        // build base BoolQuery once (immutable BoolQuery)
        BoolQuery baseBool = buildBaseQuery(request);

        // put baseBool into context so all helpers use it (clone when mutating)
        SelectionContext context = new SelectionContext(request, targetCounts, baseBool);

        if (request.isIncludePassage()) {
            return selectWithPassageGroupsBalanced(context);
        } else {
            return selectIndependentItemsOnly(context);
        }
    }

    // -------------------------- utility / helpers ----------------------------
    private Map<Long, Integer> calculateTargetCounts(DifficultyDistribution distribution, Integer totalCount) {
        Map<Long, Integer> targetCounts = new HashMap<>();

        for (Map.Entry<Long, Double> entry : distribution.getDistribution().entrySet()) {
            Long difficultyCode = mapDifficultyLevel(entry.getKey());
            Double ratio = entry.getValue();
            int count = (int) Math.round(totalCount * ratio);
            if (count > 0) targetCounts.put(difficultyCode, count);
        }

        int calculatedTotal = targetCounts.values().stream().mapToInt(Integer::intValue).sum();
        int difference = totalCount - calculatedTotal;
        if (difference != 0) {
            Long maxDifficulty = distribution.getDistribution().entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(entry -> mapDifficultyLevel(entry.getKey()))
                    .orElse(3L);
            targetCounts.put(maxDifficulty, targetCounts.getOrDefault(maxDifficulty, 0) + difference);
        }

        log.info("난이도별 목표 수량: {}", targetCounts);
        return targetCounts;
    }

    // map distribution key (1/2/3) -> internal difficulty codes used in ES (2,3,4)
    private Long mapDifficultyLevel(Long internalLevel) {
        return switch (internalLevel.intValue()) {
            case 1 -> 2L;
            case 2 -> 3L;
            case 3 -> 4L;
            default -> 3L;
        };
    }

    // Build the base BoolQuery and return the built BoolQuery (immutable)
    private BoolQuery buildBaseQuery(SmartSelectionRequest request) {
        BoolQuery.Builder q = new BoolQuery.Builder();
        q.must(TermQuery.of(t -> t.field("subject_id").value(request.getSubjectId()))._toQuery());

        if (request.getChapters() != null && !request.getChapters().isEmpty()) {
            BoolQuery.Builder chapterQuery = new BoolQuery.Builder();
            for (Long mid : request.getChapters()) {
                chapterQuery.should(TermQuery.of(t -> t.field("medium_chapter_id").value(mid))._toQuery());
            }
            chapterQuery.minimumShouldMatch("1");
            // add chapterQuery as a sub-bool (we build chapterQuery once here)
            q.must(chapterQuery.build()._toQuery());
        }

        return q.build();
    }

    private static BoolQuery.Builder cloneFromBase(BoolQuery base) {
        BoolQuery.Builder clone = new BoolQuery.Builder();
        if (base == null) return clone;

        if (base.must() != null) base.must().forEach(clone::must);
        if (base.mustNot() != null) base.mustNot().forEach(clone::mustNot);
        if (base.should() != null) base.should().forEach(clone::should);
        if (base.filter() != null) base.filter().forEach(clone::filter);
        if (base.minimumShouldMatch() != null) clone.minimumShouldMatch(base.minimumShouldMatch());
        return clone;
    }

    @Getter
    @AllArgsConstructor
    private static class SelectionContext {
        private final SmartSelectionRequest request;
        private final Map<Long, Integer> targetCounts;
        private final BoolQuery baseQuery; // built BoolQuery (immutable) — clone when mutating
        private final Set<Long> selectedItemIds; // 전역 중복 방지용

        public SelectionContext(SmartSelectionRequest request, Map<Long, Integer> targetCounts, BoolQuery baseQuery) {
            this.request = request;
            this.targetCounts = targetCounts;
            this.baseQuery = baseQuery;
            this.selectedItemIds = new HashSet<>();
        }
    }

    @Getter
    @AllArgsConstructor
    private static class PassageGroupCandidate {
        private final Long passageId;
        private final List<ItemImageDocument> items;
        private final int itemCount;
        private final Long difficultyCode;
    }

    // -------------------- independent selection helpers ----------------------
    private SmartSelectionResponse selectIndependentItemsOnly(SelectionContext context) throws IOException {
        log.info("독립 문항만 선택 시작");

        Map<Long, Integer> remainingCounts = new HashMap<>(context.getTargetCounts());
        List<ItemImageDocument> selectedItems = selectIndependentItems(context, remainingCounts, new ArrayList<>());

        // 최종 중복 제거
        if (context.getRequest().isAvoidDuplicate()) {
            selectedItems = removeDuplicates(selectedItems);
        }

        Map<Long, Integer> actualItemCounts = selectedItems.stream()
                .collect(Collectors.groupingBy(ItemImageDocument::getDifficultyCode, Collectors.summingInt(i -> 1)));

        return buildSmartSelectionResponse(selectedItems, Collections.emptyList(), context.getTargetCounts(), actualItemCounts, new ArrayList<>(), context.getRequest());
    }

    /**
     * Select independent items with enhanced duplicate prevention.
     */
    private List<ItemImageDocument> selectIndependentItems(SelectionContext context,
                                                           Map<Long, Integer> remainingCounts,
                                                           List<SmartSelectionResponse.FallbackAction> fallbackActions) throws IOException {

        List<ItemImageDocument> result = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : remainingCounts.entrySet()) {
            Long difficultyCode = entry.getKey();
            Integer neededCount = entry.getValue();

            if (neededCount == null || neededCount <= 0) continue;

            log.info("난이도 {} 독립 문항 검색 - 필요 수량: {}", difficultyCode, neededCount);

            // clone builder for per-difficulty query (fresh builder each loop)
            BoolQuery.Builder q = cloneFromBase(context.getBaseQuery());
            q.must(TermQuery.of(t -> t.field("difficulty_code").value(difficultyCode))._toQuery());
            q.mustNot(ExistsQuery.of(e -> e.field("passage_id"))._toQuery());

            // 기존 선택된 항목들 제외
            if (context.getRequest().isAvoidDuplicate() && !context.getSelectedItemIds().isEmpty()) {
                for (Long itemId : context.getSelectedItemIds()) {
                    q.mustNot(TermQuery.of(t -> t.field("item_id").value(itemId))._toQuery());
                }
            }

            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(INDEX_NAME)
                    .size(Math.min(neededCount * 3, MAX_SEARCH_SIZE)) // 여유분 확보
                    .query(q.build()._toQuery())
            );

            SearchResponse<ItemImageDocument> response = elasticsearchClient.search(searchRequest, ItemImageDocument.class);
            List<ItemImageDocument> candidates = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // 추가 중복 체크 (메모리 기반)
            if (context.getRequest().isAvoidDuplicate()) {
                Set<Long> alreadySelected = new HashSet<>(context.getSelectedItemIds());
                result.stream().forEach(item -> alreadySelected.add(item.getItemId()));
                candidates = candidates.stream()
                        .filter(item -> !alreadySelected.contains(item.getItemId()))
                        .collect(Collectors.toList());
            }
            Collections.shuffle(candidates, random);

            int actualSelected = Math.min(neededCount, candidates.size());
            List<ItemImageDocument> selectedForThisDifficulty = candidates.subList(0, actualSelected);
            result.addAll(selectedForThisDifficulty);

            // 선택된 항목들을 전역 세트에 추가
            selectedForThisDifficulty.forEach(item -> context.getSelectedItemIds().add(item.getItemId()));

            if (actualSelected < neededCount) {
                fallbackActions.add(SmartSelectionResponse.FallbackAction.builder()
                        .action("INSUFFICIENT_ITEMS")
                        .fromDifficulty(difficultyCode)
                        .toDifficulty(difficultyCode)
                        .count(neededCount - actualSelected)
                        .reason(String.format("난이도 %d 독립 문항 부족 (%d개 요청, %d개 선택)", difficultyCode, neededCount, actualSelected))
                        .build());
            }

            log.info("난이도 {} 독립 문항 {}개 선택 (요청: {}개)", difficultyCode, actualSelected, neededCount);
        }

        return result;
    }

    // ----------------------- main flow: passage groups -----------------------
    private SmartSelectionResponse selectWithPassageGroups(SelectionContext context) throws IOException {

        List<ItemImageDocument> selectedItems = new ArrayList<>();
        List<SmartSelectionResponse.PassageGroupInfo> passageGroups = new ArrayList<>();
        List<SmartSelectionResponse.FallbackAction> fallbackActions = new ArrayList<>();

        Map<Long, Integer> remainingCounts = new HashMap<>(context.getTargetCounts());

        // 0) Precheck
        SmartSelectionResponse pre = precheckAndMaybeReturnAll(context);
        if (pre != null) return pre;

        // 1) Fetch passage candidates grouped by passage_id for each difficulty
        List<PassageGroupCandidate> allCandidates = fetchPassageCandidates(context, remainingCounts);

        // 2) DP select passages with duplicate prevention
        List<PassageGroupCandidate> selectedCandidates = selectPassageGroupsWithDP(allCandidates, remainingCounts, context);
        Set<Long> selectedPassageIds = new HashSet<>();

        for (PassageGroupCandidate c : selectedCandidates) {
            selectedPassageIds.add(c.getPassageId());

            // 중복 체크 후 유효한 항목만 추가
            List<ItemImageDocument> validItems = filterValidItems(c.getItems(), context);
            selectedItems.addAll(validItems);

            // 선택된 항목들을 전역 세트에 추가
            validItems.forEach(item -> context.getSelectedItemIds().add(item.getItemId()));

            Long diff = c.getDifficultyCode();
            remainingCounts.put(diff, Math.max(0, remainingCounts.getOrDefault(diff, 0) - validItems.size()));

            passageGroups.add(SmartSelectionResponse.PassageGroupInfo.builder()
                    .passageId(c.getPassageId())
                    .itemCount(validItems.size())
                    .representativeDifficulty(getMostCommonDifficulty(validItems))
                    .itemIds(validItems.stream().map(ItemImageDocument::getItemId).collect(Collectors.toList()))
                    .build());
        }

        int requestedTotal = context.getRequest().getItemCount();

        // 3) Fill with independents
        List<ItemImageDocument> independents = selectIndependentItems(context, remainingCounts, fallbackActions);
        selectedItems.addAll(independents);

        // 4) If still short -> greedy add passages
        while (selectedItems.size() < requestedTotal) {
            int shortage = requestedTotal - selectedItems.size();

            List<PassageGroupCandidate> remainingCandidates = allCandidates.stream()
                    .filter(c -> !selectedPassageIds.contains(c.getPassageId()))
                    .collect(Collectors.toList());

            int added = addAdditionalPassagesIfNeeded(remainingCandidates, shortage, remainingCounts,
                    selectedItems, passageGroups, requestedTotal, context);

            // 부족분 발생 시 독립 문항으로 보충
            if (selectedItems.size() < requestedTotal) {
                int remainingShortage = requestedTotal - selectedItems.size();
                List<ItemImageDocument> additionalIndependents = selectIndependentItems(context, remainingCounts, fallbackActions);
                int actuallyAdded = Math.min(remainingShortage, additionalIndependents.size());
                selectedItems.addAll(additionalIndependents.subList(0, actuallyAdded));

                // 부족분 발생 시 fallback 기록
                if (actuallyAdded < remainingShortage) {
                    fallbackActions.add(SmartSelectionResponse.FallbackAction.builder()
                            .action("INSUFFICIENT_TOTAL_ITEMS_AFTER_ADJUST")
                            .count(remainingShortage - actuallyAdded)
                            .reason(String.format("독립문항 + 추가 지문으로도 요청 문항수 %d를 채우지 못함 (남음 %d)", requestedTotal, remainingShortage - actuallyAdded))
                            .build());
                }
            }

            // 더 이상 추가할 수 없으면 종료
            if (added == 0) break;
        }

        // 5) soft ratio adjust...
        double targetPassageRatio = 0.3;
        long currentPassageItems = selectedItems.stream().filter(it -> it.getPassageId() != null).count();
        int totalSelected = selectedItems.size();
        double currentRatio = totalSelected > 0 ? (double) currentPassageItems / totalSelected : 0.0;
        int desiredPassageItems = (int) Math.round(requestedTotal * targetPassageRatio);

        if (currentRatio > targetPassageRatio + 0.05) {
            int needToReplace = Math.max(0, (int) currentPassageItems - desiredPassageItems);
            boolean replaced = tryReducePassagesByReplacingWithIndependents(selectedCandidates, context, needToReplace, selectedItems, passageGroups, fallbackActions, requestedTotal);
            if (!replaced) {
                fallbackActions.add(SmartSelectionResponse.FallbackAction.builder()
                        .action("RATIO_UNABLE_ADJUST")
                        .reason(String.format("지문:독립 비율 목표(%.2f)로 조정 불가 (현재 %.2f)", targetPassageRatio, currentRatio))
                        .build());
            }
        }

        // 최종 중복 제거
        if (context.getRequest().isAvoidDuplicate()) {
            selectedItems = removeDuplicates(selectedItems);
        }

        Map<Long, Integer> actualItemCounts = selectedItems.stream()
                .collect(Collectors.groupingBy(ItemImageDocument::getDifficultyCode, Collectors.summingInt(it -> 1)));

        return buildSmartSelectionResponse(selectedItems, passageGroups, context.getTargetCounts(), actualItemCounts, fallbackActions, context.getRequest());
    }

    // ---------------- 중복 제거 및 유효성 검사 헬퍼 메서드들 ----------------
    private List<ItemImageDocument> filterValidItems(List<ItemImageDocument> items, SelectionContext context) {
        if (!context.getRequest().isAvoidDuplicate()) {
            return items;
        }

        return items.stream()
                .filter(item -> !context.getSelectedItemIds().contains(item.getItemId()))
                .collect(Collectors.toList());
    }

    private List<ItemImageDocument> removeDuplicates(List<ItemImageDocument> items) {
        Set<Long> seen = new HashSet<>();
        return items.stream()
                .filter(item -> seen.add(item.getItemId()))
                .collect(Collectors.toList());
    }

    // ---------------- passage candidates 조회 ----------------
    private List<PassageGroupCandidate> fetchPassageCandidates(SelectionContext context, Map<Long, Integer> remainingCounts) throws IOException {
        List<PassageGroupCandidate> allCandidates = new ArrayList<>();
        BoolQuery baseBool = context.getBaseQuery();

        for (Long difficultyCode : new ArrayList<>(remainingCounts.keySet())) {
            int neededCount = remainingCounts.getOrDefault(difficultyCode, 0);
            if (neededCount <= 0) continue;

            // clone base and add difficulty + passage exists
            BoolQuery.Builder difficultyQBuilder = cloneFromBase(baseBool);
            difficultyQBuilder.must(TermQuery.of(t -> t.field("difficulty_code").value(difficultyCode))._toQuery());
            difficultyQBuilder.must(ExistsQuery.of(e -> e.field("passage_id"))._toQuery());

            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(INDEX_NAME)
                    .size(0)
                    .query(difficultyQBuilder.build()._toQuery())
                    .aggregations("passages", Aggregation.of(a -> a
                            .terms(TermsAggregation.of(t -> t.field("passage_id").size(MAX_SEARCH_SIZE)))
                            .aggregations("passage_items", Aggregation.of(sub -> sub.topHits(th -> th.size(50))))
                    ))
            );

            SearchResponse<ItemImageDocument> response = elasticsearchClient.search(searchRequest, ItemImageDocument.class);
            Aggregate passagesAgg = response.aggregations().get("passages");

            if (passagesAgg == null) continue;

            // sterms or lterms handling
            processPassageAggregation(passagesAgg, difficultyCode, allCandidates);
        }
        Collections.shuffle(allCandidates, random);

        return allCandidates;
    }

    private void processPassageAggregation(Aggregate passagesAgg, Long difficultyCode, List<PassageGroupCandidate> allCandidates) {
        try {
            var sterms = passagesAgg.sterms();
            for (var bucket : sterms.buckets().array()) {
                Long passageId = null;
                try {
                    passageId = Long.valueOf(bucket.key().stringValue());
                } catch (Exception ex) {
                    log.debug("passage bucket key -> long failed for sterms: {}", bucket.key(), ex);
                    continue;
                }

                var topHitsAgg = bucket.aggregations().get("passage_items").topHits();
                List<ItemImageDocument> items = topHitsAgg.hits().hits().stream()
                        .map(hit -> {
                            try {
                                if (hit.source() != null) {
                                    return objectMapper.readValue(hit.source().toString(), ItemImageDocument.class);
                                } else {
                                    return null;
                                }
                            } catch (Exception e) {
                                log.error("Failed to parse topHit source to ItemImageDocument (sterms)", e);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                if (!items.isEmpty()) {
                    allCandidates.add(new PassageGroupCandidate(passageId, items, items.size(), difficultyCode));
                }
            }
        } catch (IllegalStateException ignore) {
            // try lterms
            try {
                var lterms = passagesAgg.lterms();
                for (var bucket : lterms.buckets().array()) {
                    Long passageId = bucket.key();

                    var passageTopHits = bucket.aggregations().get("passage_items").topHits();
                    List<ItemImageDocument> items = passageTopHits.hits().hits().stream()
                            .map(hit -> {
                                try {
                                    if (hit.source() != null) {
                                        return hit.source().to(ItemImageDocument.class);
                                    } else {
                                        return null;
                                    }
                                } catch (Exception e) {
                                    log.error("Failed to parse topHit source to ItemImageDocument (lterms)", e);
                                    return null;
                                }
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    if (!items.isEmpty()) {
                        allCandidates.add(new PassageGroupCandidate(passageId, items, items.size(), difficultyCode));
                    }
                }
            } catch (IllegalStateException ex2) {
                log.warn("passages aggregation returned unknown terms variant: {}", ex2.getMessage());
            }
        }
    }

    // ---------------- precheck: if total available <= requested, return all available ----------------
    private SmartSelectionResponse precheckAndMaybeReturnAll(SelectionContext context) throws IOException {
        int requestedTotal = context.getRequest().getItemCount();
        int totalAvailable = 0;
        BoolQuery baseBool = context.getBaseQuery();

        for (Long difficultyCode : context.getTargetCounts().keySet()) {
            BoolQuery.Builder q = cloneFromBase(baseBool);
            q.must(TermQuery.of(t -> t.field("difficulty_code").value(difficultyCode))._toQuery());

            SearchRequest countReq = SearchRequest.of(s -> s.index(INDEX_NAME).size(0).query(q.build()._toQuery()));
            SearchResponse<ItemImageDocument> countResp = elasticsearchClient.search(countReq, ItemImageDocument.class);
            long cnt = countResp.hits().total() != null ? countResp.hits().total().value() : 0L;
            totalAvailable += (int) cnt;
        }

        log.info("사전검사: 요청 {}개, 가용 총합 {}개", requestedTotal, totalAvailable);

        if (totalAvailable <= requestedTotal) {
            BoolQuery.Builder allQ = cloneFromBase(baseBool);
            int finalTotalAvailable = totalAvailable;

            SearchRequest fetchAll = SearchRequest.of(s -> s
                    .index(INDEX_NAME)
                    .size(Math.min(finalTotalAvailable, MAX_SEARCH_SIZE))
                    .query(allQ.build()._toQuery())
                    .sort(Collections.singletonList(SortOptions.of(f -> f.field(FieldSort.of(fs -> fs.field("_score").order(SortOrder.Desc)))))));

            SearchResponse<ItemImageDocument> allResp = elasticsearchClient.search(fetchAll, ItemImageDocument.class);
            List<ItemImageDocument> items = allResp.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            Collections.shuffle(items, random);

            // 중복 제거
            if (context.getRequest().isAvoidDuplicate()) {
                items = removeDuplicates(items);
            }

            // passageId 기준으로 두 그룹 분리
            Map<Boolean, List<ItemImageDocument>> partitioned = items.stream()
                    .collect(Collectors.partitioningBy(item -> item.getPassageId() != null));

            List<ItemImageDocument> passageItems = partitioned.get(true);
            List<ItemImageDocument> independentItems = partitioned.get(false);

            // PassageGroupInfo 생성
            List<SmartSelectionResponse.PassageGroupInfo> passageGroups = passageItems.stream()
                    .collect(Collectors.groupingBy(ItemImageDocument::getPassageId))
                    .entrySet().stream()
                    .map(e -> {
                        List<ItemImageDocument> groupItems = e.getValue();
                        return SmartSelectionResponse.PassageGroupInfo.builder()
                                .passageId(e.getKey())
                                .itemCount(groupItems.size())
                                .representativeDifficulty(getMostCommonDifficulty(groupItems))
                                .itemIds(groupItems.stream().map(ItemImageDocument::getItemId).collect(Collectors.toList()))
                                .build();
                    })
                    .collect(Collectors.toList());

            // 난이도별 실제 카운트 계산
            Map<Long, Integer> actualItemCounts = items.stream()
                    .collect(Collectors.groupingBy(ItemImageDocument::getDifficultyCode, Collectors.summingInt(i -> 1)));

            // FallbackAction 기록
            List<SmartSelectionResponse.FallbackAction> fallbackActions = new ArrayList<>();
            fallbackActions.add(SmartSelectionResponse.FallbackAction.builder()
                    .action("INSUFFICIENT_TOTAL_ITEMS")
                    .count(requestedTotal - items.size())
                    .reason("요청된 문항수보다 전체 가용 문항수가 적음 - 가능한 항목만 반환")
                    .build());

            // Metadata 생성
            int selectionUnitCount = independentItems.size() + passageGroups.size();
            Map<Long, SmartSelectionResponse.DifficultyInfo> difficultyDistribution = buildDifficultyDistribution(
                    context.getTargetCounts(), actualItemCounts, independentItems, passageGroups, requestedTotal
            );

            SmartSelectionResponse.SmartSelectionMetadata metadata = SmartSelectionResponse.SmartSelectionMetadata.builder()
                    .requestedCount(requestedTotal)
                    .actualItemCount(items.size())
                    .selectionUnitCount(selectionUnitCount)
                    .passageGroupCount(passageGroups.size())
                    .difficultyDistribution(difficultyDistribution)
                    .passageGroups(passageGroups)
                    .fallbackActions(fallbackActions)
                    .build();

            // Report 생성
            double accuracy = calculateDistributionAccuracy(context.getTargetCounts(), actualItemCounts, requestedTotal);
            List<String> warnings = generateWarnings(items.size(), requestedTotal, fallbackActions);
            SmartSelectionResponse.SmartSelectionReport report = SmartSelectionResponse.SmartSelectionReport.builder()
                    .success(false) // 요청 개수 달성 불가
                    .message(String.format("%d개 문항만 선택 가능 (전체 가용 문항수 부족 / 지문 그룹 %d개 포함)", items.size(), passageGroups.size()))
                    .warnings(warnings)
                    .distributionAccuracy(accuracy)
                    .build();

            return SmartSelectionResponse.builder()
                    .items(items.stream().map(this::convertToItemSearchResponse).collect(Collectors.toList()))
                    .metadata(metadata)
                    .report(report)
                    .build();
        }
        return null;
    }

    private Map<Long, SmartSelectionResponse.DifficultyInfo> buildDifficultyDistribution(
            Map<Long, Integer> targetCounts,
            Map<Long, Integer> actualItemCounts,
            List<ItemImageDocument> independentItems,
            List<SmartSelectionResponse.PassageGroupInfo> passageGroups,
            int requestedTotal) {

        // 1) 독립 문항 난이도별 집계
        Map<Long, Long> independentCountsByDiff = independentItems.stream()
                .collect(Collectors.groupingBy(ItemImageDocument::getDifficultyCode, Collectors.counting()));

        // 2) 지문 그룹 난이도별 집계 (대표 난이도로 합산)
        Map<Long, Long> passageGroupCountsByDiff = passageGroups.stream()
                .collect(Collectors.groupingBy(
                        SmartSelectionResponse.PassageGroupInfo::getRepresentativeDifficulty,
                        Collectors.summingLong(SmartSelectionResponse.PassageGroupInfo::getItemCount)
                ));

        // 3) 모든 난이도 코드 집합
        Set<Long> allDiffs = new TreeSet<>();
        if (targetCounts != null) allDiffs.addAll(targetCounts.keySet());
        allDiffs.addAll(independentCountsByDiff.keySet());
        allDiffs.addAll(passageGroupCountsByDiff.keySet());

        // 4) DifficultyInfo 생성
        Map<Long, SmartSelectionResponse.DifficultyInfo> difficultyDistribution = new LinkedHashMap<>();
        for (Long diff : allDiffs) {
            int independent = independentCountsByDiff.getOrDefault(diff, 0L).intValue();
            int pgCount = passageGroupCountsByDiff.getOrDefault(diff, 0L).intValue();
            int target = targetCounts.getOrDefault(diff, 0);
            int actualForReport = independent + pgCount;

            double targetPct = requestedTotal > 0 ? (target * 100.0 / requestedTotal) : 0.0;
            double actualPct = requestedTotal > 0 ? (actualForReport * 100.0 / requestedTotal) : 0.0;

            SmartSelectionResponse.DifficultyInfo info = SmartSelectionResponse.DifficultyInfo.builder()
                    .difficultyCode(diff)
                    .difficultyName(getDifficultyName(diff))
                    .targetCount(target)
                    .actualCount(actualForReport)
                    .independentItems(independent)
                    .passageGroups(pgCount)
                    .targetPercentage(targetPct)
                    .actualPercentage(actualPct)
                    .build();

            difficultyDistribution.put(diff, info);
        }

        return difficultyDistribution;
    }


    private List<PassageGroupCandidate> selectPassageGroupsWithDP(List<PassageGroupCandidate> candidates,
                                                                  Map<Long, Integer> targetCounts,
                                                                  SelectionContext context) {

        // 먼저 난이도별 균형 선택 시도
        List<PassageGroupCandidate> balancedSelection = tryBalancedPassageSelection(candidates, targetCounts, context);
        if (!balancedSelection.isEmpty()) {
            log.info("균형잡힌 지문 선택 성공: {}개 그룹", balancedSelection.size());
            return balancedSelection;
        }

        // 균형 선택 실패시 기존 DP 로직 사용
        return selectPassageGroupsWithOriginalDP(candidates, targetCounts, context);
    }

    // 난이도별 균형을 고려한 지문 그룹 선택
    private List<PassageGroupCandidate> tryBalancedPassageSelection(List<PassageGroupCandidate> candidates,
                                                                    Map<Long, Integer> targetCounts,
                                                                    SelectionContext context) {
        // 중복 제거된 후보군 생성
        List<PassageGroupCandidate> validCandidates = new ArrayList<>();

        if (context.getRequest().isAvoidDuplicate()) {
            for (PassageGroupCandidate candidate : candidates) {
                List<ItemImageDocument> validItems = filterValidItems(candidate.getItems(), context);
                if (!validItems.isEmpty()) {
                    validCandidates.add(new PassageGroupCandidate(
                            candidate.getPassageId(),
                            validItems,
                            validItems.size(),
                            candidate.getDifficultyCode()
                    ));
                }
            }
        } else {
            validCandidates = candidates;
        }

        // 난이도별로 그룹핑
        Map<Long, List<PassageGroupCandidate>> byDifficulty = validCandidates.stream()
                .collect(Collectors.groupingBy(PassageGroupCandidate::getDifficultyCode));

        List<PassageGroupCandidate> selected = new ArrayList<>();
        Map<Long, Integer> remainingTargets = new HashMap<>(targetCounts);

        // 지문으로 충족 가능한 최대 비율 계산 (전체의 30% 이하로 제한)
        int totalTarget = targetCounts.values().stream().mapToInt(Integer::intValue).sum();
        int maxPassageItems = (int) Math.round(totalTarget * 0.3);

        // 우선순위: 부족한 난이도부터 (하 → 상 → 중 순서로)
        List<Long> priorityOrder = Arrays.asList(2L, 4L, 3L);

        int totalSelectedItems = 0;

        for (Long difficulty : priorityOrder) {
            if (totalSelectedItems >= maxPassageItems) break;

            List<PassageGroupCandidate> diffCandidates = byDifficulty.getOrDefault(difficulty, new ArrayList<>());
            if (diffCandidates.isEmpty()) continue;

            int targetForDiff = remainingTargets.getOrDefault(difficulty, 0);
            if (targetForDiff <= 0) continue;

            // 난이도별 지문 그룹을 효율성으로 정렬 (작은 그룹부터)
            diffCandidates.sort(Comparator.comparingInt(PassageGroupCandidate::getItemCount));

            int currentSelected = 0;
            for (PassageGroupCandidate candidate : diffCandidates) {
                if (totalSelectedItems >= maxPassageItems) break;
                if (currentSelected >= targetForDiff) break;

                int candidateSize = candidate.getItemCount();

                // 목표 개수를 초과하지 않고, 전체 제한도 넘지 않는 경우만 선택
                if (currentSelected + candidateSize <= targetForDiff &&
                        totalSelectedItems + candidateSize <= maxPassageItems) {

                    selected.add(candidate);
                    currentSelected += candidateSize;
                    totalSelectedItems += candidateSize;

                    log.info("균형 선택 - 난이도 {}: 지문 {} ({}개 문항)",
                            difficulty, candidate.getPassageId(), candidateSize);
                }
            }

            // 선택된 만큼 목표에서 차감
            remainingTargets.put(difficulty, Math.max(0, targetForDiff - currentSelected));
        }

        return selected;
    }


    // 기존 DP 로직 (백업용)
    private List<PassageGroupCandidate> selectPassageGroupsWithOriginalDP(List<PassageGroupCandidate> candidates,
                                                                          Map<Long, Integer> targetCounts,
                                                                          SelectionContext context) {
        // 중복 제거된 후보군 생성
        List<PassageGroupCandidate> validCandidates = new ArrayList<>();

        if (context.getRequest().isAvoidDuplicate()) {
            for (PassageGroupCandidate candidate : candidates) {
                List<ItemImageDocument> validItems = filterValidItems(candidate.getItems(), context);
                if (!validItems.isEmpty()) {
                    validCandidates.add(new PassageGroupCandidate(
                            candidate.getPassageId(),
                            validItems,
                            validItems.size(),
                            candidate.getDifficultyCode()
                    ));
                }
            }
        } else {
            validCandidates = candidates;
        }

        int n = validCandidates.size();
        int maxD2 = targetCounts.getOrDefault(2L, 0);
        int maxD3 = targetCounts.getOrDefault(3L, 0);
        int maxD4 = targetCounts.getOrDefault(4L, 0);

        int[][][][] dp = new int[n + 1][maxD2 + 1][maxD3 + 1][maxD4 + 1];
        boolean[][][][] take = new boolean[n + 1][maxD2 + 1][maxD3 + 1][maxD4 + 1];

        for (int i = 1; i <= n; i++) {
            PassageGroupCandidate c = validCandidates.get(i - 1);
            int cD2 = c.getDifficultyCode() == 2L ? c.getItemCount() : 0;
            int cD3 = c.getDifficultyCode() == 3L ? c.getItemCount() : 0;
            int cD4 = c.getDifficultyCode() == 4L ? c.getItemCount() : 0;

            for (int d2 = 0; d2 <= maxD2; d2++) {
                for (int d3 = 0; d3 <= maxD3; d3++) {
                    for (int d4 = 0; d4 <= maxD4; d4++) {
                        dp[i][d2][d3][d4] = dp[i - 1][d2][d3][d4];
                        if (d2 >= cD2 && d3 >= cD3 && d4 >= cD4) {
                            int val = dp[i - 1][d2 - cD2][d3 - cD3][d4 - cD4] + c.getItemCount();
                            if (val > dp[i][d2][d3][d4]) {
                                dp[i][d2][d3][d4] = val;
                                take[i][d2][d3][d4] = true;
                            }
                        }
                    }
                }
            }
        }

        List<PassageGroupCandidate> selected = new ArrayList<>();
        int d2 = maxD2, d3 = maxD3, d4 = maxD4;
        for (int i = n; i >= 1; i--) {
            if (take[i][d2][d3][d4]) {
                PassageGroupCandidate c = validCandidates.get(i - 1);
                selected.add(c);
                if (c.getDifficultyCode() == 2L) d2 -= c.getItemCount();
                if (c.getDifficultyCode() == 3L) d3 -= c.getItemCount();
                if (c.getDifficultyCode() == 4L) d4 -= c.getItemCount();
            }
        }
        return selected;
    }

    // 실제 선택된 문항을 기반으로 남은 목표 개수 재계산
    private Map<Long, Integer> recalculateRemainingCounts(Map<Long, Integer> originalTargets,
                                                          List<ItemImageDocument> selectedItems) {
        // 실제 선택된 난이도별 개수 계산
        Map<Long, Integer> actualCounts = selectedItems.stream()
                .collect(Collectors.groupingBy(
                        ItemImageDocument::getDifficultyCode,
                        Collectors.summingInt(i -> 1)
                ));

        Map<Long, Integer> remaining = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : originalTargets.entrySet()) {
            Long difficulty = entry.getKey();
            int target = entry.getValue();
            int actual = actualCounts.getOrDefault(difficulty, 0);
            int need = Math.max(0, target - actual);

            if (need > 0) {
                remaining.put(difficulty, need);
            }
        }

        log.info("목표 재계산 - 원본: {}, 실제: {}, 남은 목표: {}",
                originalTargets, actualCounts, remaining);

        return remaining;
    }

    // 개선된 독립 문항 선택 (과다 선택 방지)
    private List<ItemImageDocument> selectIndependentItemsBalanced(SelectionContext context,
                                                                   List<ItemImageDocument> alreadySelected,
                                                                   List<SmartSelectionResponse.FallbackAction> fallbackActions) throws IOException {

        // 이미 선택된 문항 기준으로 남은 목표 재계산
        Map<Long, Integer> remainingCounts = recalculateRemainingCounts(
                context.getTargetCounts(), alreadySelected);

        if (remainingCounts.isEmpty()) {
            log.info("모든 난이도 목표 달성 - 추가 독립 문항 선택 불필요");
            return new ArrayList<>();
        }

        return selectIndependentItems(context, remainingCounts, fallbackActions);
    }

    // selectWithPassageGroups 메서드의 독립 문항 선택 부분 수정
    private SmartSelectionResponse selectWithPassageGroupsBalanced(SelectionContext context) throws IOException {
        BoolQuery baseBool = context.getBaseQuery();

        List<ItemImageDocument> selectedItems = new ArrayList<>();
        List<SmartSelectionResponse.PassageGroupInfo> passageGroups = new ArrayList<>();
        List<SmartSelectionResponse.FallbackAction> fallbackActions = new ArrayList<>();

        Map<Long, Integer> remainingCounts = new HashMap<>(context.getTargetCounts());

        // 0) Precheck
        SmartSelectionResponse pre = precheckAndMaybeReturnAll(context);
        if (pre != null) return pre;

        // 1) Fetch passage candidates grouped by passage_id for each difficulty
        List<PassageGroupCandidate> allCandidates = fetchPassageCandidates(context, remainingCounts);

        // 2) Balanced passage selection
        List<PassageGroupCandidate> selectedCandidates = selectPassageGroupsWithDP(allCandidates, remainingCounts, context);
        Set<Long> selectedPassageIds = new HashSet<>();

        for (PassageGroupCandidate c : selectedCandidates) {
            selectedPassageIds.add(c.getPassageId());

            // 중복 체크 후 유효한 항목만 추가
            List<ItemImageDocument> validItems = filterValidItems(c.getItems(), context);
            selectedItems.addAll(validItems);

            // 선택된 항목들을 전역 세트에 추가
            validItems.forEach(item -> context.getSelectedItemIds().add(item.getItemId()));

            passageGroups.add(SmartSelectionResponse.PassageGroupInfo.builder()
                    .passageId(c.getPassageId())
                    .itemCount(validItems.size())
                    .representativeDifficulty(getMostCommonDifficulty(validItems))
                    .itemIds(validItems.stream().map(ItemImageDocument::getItemId).collect(Collectors.toList()))
                    .build());
        }

        int requestedTotal = context.getRequest().getItemCount();

        // 3) Fill with balanced independent items
        List<ItemImageDocument> independents = selectIndependentItemsBalanced(context, selectedItems, fallbackActions);
        selectedItems.addAll(independents);

        // 4) 부족분 체크 및 최소한의 추가 선택
        if (selectedItems.size() < requestedTotal) {
            int shortage = requestedTotal - selectedItems.size();
            log.info("부족분 발생: {}개 - 최소한의 추가 선택 시도", shortage);

            // 부족한 만큼만 추가로 독립 문항 선택 (난이도 무시하고)
            List<ItemImageDocument> additionalItems = selectAnyAvailableIndependentItems(
                    context, shortage, fallbackActions);
            selectedItems.addAll(additionalItems);

            if (selectedItems.size() < requestedTotal) {
                int finalShortage = requestedTotal - selectedItems.size();
                fallbackActions.add(SmartSelectionResponse.FallbackAction.builder()
                        .action("INSUFFICIENT_TOTAL_ITEMS_FINAL")
                        .count(finalShortage)
                        .reason(String.format("최종 부족분 %d개 - 더 이상 선택할 문항 없음", finalShortage))
                        .build());
            }
        }

        // 최종 중복 제거
        if (context.getRequest().isAvoidDuplicate()) {
            int beforeSize = selectedItems.size();
            selectedItems = removeDuplicates(selectedItems);
            int afterSize = selectedItems.size();
            if (beforeSize != afterSize) {
                log.warn("최종 중복 제거로 {}개 문항 감소", beforeSize - afterSize);
            }
        }

        Map<Long, Integer> actualItemCounts = selectedItems.stream()
                .collect(Collectors.groupingBy(ItemImageDocument::getDifficultyCode, Collectors.summingInt(it -> 1)));

        return buildSmartSelectionResponse(selectedItems, passageGroups, context.getTargetCounts(), actualItemCounts, fallbackActions, context.getRequest());
    }

    // 난이도 무시하고 사용 가능한 독립 문항 선택 (부족분 해결용)
    private List<ItemImageDocument> selectAnyAvailableIndependentItems(SelectionContext context,
                                                                       int needCount,
                                                                       List<SmartSelectionResponse.FallbackAction> fallbackActions) throws IOException {
        if (needCount <= 0) return new ArrayList<>();

        BoolQuery.Builder q = cloneFromBase(context.getBaseQuery());
        q.mustNot(ExistsQuery.of(e -> e.field("passage_id"))._toQuery());

        // 기존 선택된 항목들 제외
        if (context.getRequest().isAvoidDuplicate() && !context.getSelectedItemIds().isEmpty()) {
            for (Long itemId : context.getSelectedItemIds()) {
                q.mustNot(TermQuery.of(t -> t.field("item_id").value(itemId))._toQuery());
            }
        }

        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(INDEX_NAME)
                .size(Math.min(needCount * 2, MAX_SEARCH_SIZE))
                .query(q.build()._toQuery())
        );

        SearchResponse<ItemImageDocument> response = elasticsearchClient.search(searchRequest, ItemImageDocument.class);
        List<ItemImageDocument> candidates = response.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 추가 중복 체크
        if (context.getRequest().isAvoidDuplicate()) {
            candidates = candidates.stream()
                    .filter(item -> !context.getSelectedItemIds().contains(item.getItemId()))
                    .collect(Collectors.toList());
        }
        Collections.shuffle(candidates, random);

        int actualSelected = Math.min(needCount, candidates.size());
        List<ItemImageDocument> selected = candidates.subList(0, actualSelected);

        // 전역 세트에 추가
        selected.forEach(item -> context.getSelectedItemIds().add(item.getItemId()));

        log.info("추가 독립 문항 {}개 선택 (요청: {}개)", actualSelected, needCount);

        return selected;
    }

    // -------------------- add additional passages greedily with duplicate prevention -------------------
    private int addAdditionalPassagesIfNeeded(List<PassageGroupCandidate> remainingCandidates,
                                              int shortage,
                                              Map<Long, Integer> remainingCounts,
                                              List<ItemImageDocument> selectedItems,
                                              List<SmartSelectionResponse.PassageGroupInfo> passageGroups,
                                              int requestedTotal,
                                              SelectionContext context) {

        List<PassageGroupCandidate> pool = new ArrayList<>();

        // 중복이 없는 후보군만 추가
        for (PassageGroupCandidate candidate : remainingCandidates) {
            List<ItemImageDocument> validItems = filterValidItems(candidate.getItems(), context);
            if (!validItems.isEmpty()) {
                pool.add(new PassageGroupCandidate(
                        candidate.getPassageId(),
                        validItems,
                        validItems.size(),
                        candidate.getDifficultyCode()
                ));
            }
        }

        pool.sort((a, b) -> {
            int scoreA = Math.min(a.getItemCount(), remainingCounts.getOrDefault(a.getDifficultyCode(), 0));
            int scoreB = Math.min(b.getItemCount(), remainingCounts.getOrDefault(b.getDifficultyCode(), 0));
            return Integer.compare(scoreB, scoreA);
        });

        int addedCount = 0;
        for (PassageGroupCandidate c : pool) {
            if (addedCount >= shortage) break;
            if (selectedItems.size() + c.getItemCount() > requestedTotal) continue;

            selectedItems.addAll(c.getItems());

            // 전역 중복 방지 세트에 추가
            c.getItems().forEach(item -> context.getSelectedItemIds().add(item.getItemId()));

            passageGroups.add(SmartSelectionResponse.PassageGroupInfo.builder()
                    .passageId(c.getPassageId())
                    .itemCount(c.getItemCount())
                    .representativeDifficulty(getMostCommonDifficulty(c.getItems()))
                    .itemIds(c.getItems().stream().map(ItemImageDocument::getItemId).collect(Collectors.toList()))
                    .build());

            Long diff = c.getDifficultyCode();
            remainingCounts.put(diff, Math.max(0, remainingCounts.getOrDefault(diff, 0) - c.getItemCount()));

            addedCount += c.getItemCount();
            log.info("추가 지문 선택: {} (난이도 {} / {}개)", c.getPassageId(), c.getDifficultyCode(), c.getItemCount());
        }
        return addedCount;
    }

    // -------------------- try reduce passages by replacing with independents ---
    private boolean tryReducePassagesByReplacingWithIndependents(
            List<PassageGroupCandidate> selectedCandidates,
            SelectionContext context,
            int needToReplace,
            List<ItemImageDocument> selectedItems,
            List<SmartSelectionResponse.PassageGroupInfo> passageGroups,
            List<SmartSelectionResponse.FallbackAction> fallbackActions,
            int requestedTotal) throws IOException {

        if (needToReplace <= 0) return true;

        // build indep query cloned from base
        BoolQuery.Builder indepQueryBuilder = cloneFromBase(context.getBaseQuery());
        indepQueryBuilder.mustNot(ExistsQuery.of(e -> e.field("passage_id"))._toQuery());

        // 중복 제거를 위한 mustNot 추가
        if (context.getRequest().isAvoidDuplicate() && !context.getSelectedItemIds().isEmpty()) {
            for (Long itemId : context.getSelectedItemIds()) {
                indepQueryBuilder.mustNot(TermQuery.of(t -> t.field("item_id").value(itemId))._toQuery());
            }
        }

        SearchRequest indepCountReq = SearchRequest.of(s -> s.index(INDEX_NAME).size(0).query(indepQueryBuilder.build()._toQuery()));
        SearchResponse<ItemImageDocument> indepCountResp = elasticsearchClient.search(indepCountReq, ItemImageDocument.class);
        long totalIndependentAvailable = indepCountResp.hits().total() != null ? indepCountResp.hits().total().value() : 0L;

        long alreadyIndependentSelected = selectedItems.stream().filter(it -> it.getPassageId() == null).count();
        long independentFree = totalIndependentAvailable - alreadyIndependentSelected;
        if (independentFree <= 0) {
            log.info("대체 불가: 사용 가능한 추가 독립 문항이 없음");
            return false;
        }

        int canReplace = (int) Math.min(needToReplace, independentFree);
        if (canReplace <= 0) return false;

        // remove smallest passage groups first
        selectedCandidates.sort(Comparator.comparingInt(PassageGroupCandidate::getItemCount));
        int replaced = 0;
        Iterator<PassageGroupCandidate> it = selectedCandidates.iterator();
        while (it.hasNext() && replaced < canReplace) {
            PassageGroupCandidate c = it.next();
            int cCount = c.getItemCount();

            Set<Long> idsToRemove = c.getItems().stream().map(ItemImageDocument::getItemId).collect(Collectors.toSet());
            selectedItems.removeIf(itm -> idsToRemove.contains(itm.getItemId()));
            passageGroups.removeIf(pg -> pg.getPassageId().equals(c.getPassageId()));

            // 전역 중복 방지 세트에서도 제거
            idsToRemove.forEach(context.getSelectedItemIds()::remove);

            replaced += cCount;
            log.info("지문 제거 (비율조정): {} ({}개)", c.getPassageId(), cCount);
        }

        int needIndependents = replaced;
        if (needIndependents <= 0) return false;

        // fetch independents
        BoolQuery.Builder indepFetchBuilder = cloneFromBase(context.getBaseQuery());
        indepFetchBuilder.mustNot(ExistsQuery.of(e -> e.field("passage_id"))._toQuery());

        // 중복 제거를 위한 mustNot 추가
        if (context.getRequest().isAvoidDuplicate() && !context.getSelectedItemIds().isEmpty()) {
            for (Long itemId : context.getSelectedItemIds()) {
                indepFetchBuilder.mustNot(TermQuery.of(t -> t.field("item_id").value(itemId))._toQuery());
            }
        }

        SearchRequest indepFetch = SearchRequest.of(s -> s
                .index(INDEX_NAME)
                .size(Math.min(needIndependents, MAX_SEARCH_SIZE))
                .query(indepFetchBuilder.build()._toQuery())
                .sort(Collections.singletonList(SortOptions.of(f -> f.field(FieldSort.of(fs -> fs.field("_score").order(SortOrder.Desc)))))) );

        SearchResponse<ItemImageDocument> indepResp = elasticsearchClient.search(indepFetch, ItemImageDocument.class);
        List<ItemImageDocument> indepCandidates = indepResp.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 추가 중복 체크
        if (context.getRequest().isAvoidDuplicate()) {
            indepCandidates = indepCandidates.stream()
                    .filter(item -> !context.getSelectedItemIds().contains(item.getItemId()))
                    .collect(Collectors.toList());
        }

        int actualAdded = Math.min(needIndependents, indepCandidates.size());
        if (actualAdded <= 0) {
            log.info("대체 불가: 독립문항 확보 실패");
            return false;
        }

        List<ItemImageDocument> addedItems = indepCandidates.subList(0, actualAdded);
        selectedItems.addAll(addedItems);

        // 전역 중복 방지 세트에 추가
        addedItems.forEach(item -> context.getSelectedItemIds().add(item.getItemId()));

        log.info("지문 제거 후 독립문항으로 보충: {}개", actualAdded);

        return actualAdded >= needToReplace;
    }



    // -------------------- buildSmartSelectionResponse (응답 생성) -------------
    private SmartSelectionResponse buildSmartSelectionResponse(
            List<ItemImageDocument> selectedItems,
            List<SmartSelectionResponse.PassageGroupInfo> passageGroups,
            Map<Long, Integer> targetCounts,
            Map<Long, Integer> actualItemCounts,
            List<SmartSelectionResponse.FallbackAction> fallbackActions,
            SmartSelectionRequest request) {

        // 1) Item -> ItemSearchResponse 변환
        List<ItemSearchResponse> itemResponses = selectedItems.stream()
                .map(this::convertToItemSearchResponse)
                .collect(Collectors.toList());

        int requestedCount = request.getItemCount();
        int actualCount = selectedItems.size();

        // 2) Build difficulty distribution (independent items + passage group count)
        Map<Long, Long> independentCountsByDiff = selectedItems.stream()
                .filter(it -> it.getPassageId() == null)
                .collect(Collectors.groupingBy(ItemImageDocument::getDifficultyCode, Collectors.counting()));

        Map<Long, Long> passageGroupCountsByDiff = passageGroups.stream()
                .collect(Collectors.groupingBy(
                        SmartSelectionResponse.PassageGroupInfo::getRepresentativeDifficulty,
                        Collectors.summingLong(SmartSelectionResponse.PassageGroupInfo::getItemCount) // 그룹 내 문항 수 합계
                ));

        // union keys
        Set<Long> allDiffs = new TreeSet<>();
        if (targetCounts != null) allDiffs.addAll(targetCounts.keySet());
        allDiffs.addAll(independentCountsByDiff.keySet());
        allDiffs.addAll(passageGroupCountsByDiff.keySet());

        Map<Long, SmartSelectionResponse.DifficultyInfo> difficultyDistribution = new LinkedHashMap<>();
        for (Long diff : allDiffs) {
            int independent = independentCountsByDiff.getOrDefault(diff, 0L).intValue();
            int pgCount = passageGroupCountsByDiff.getOrDefault(diff, 0L).intValue();
            int target = targetCounts.getOrDefault(diff, 0);
            int actualForReport = independent + pgCount;

            double targetPct = requestedCount > 0 ? (target * 100.0 / requestedCount) : 0.0;
            double actualPct = requestedCount > 0 ? (actualForReport * 100.0 / requestedCount) : 0.0;

            SmartSelectionResponse.DifficultyInfo info = SmartSelectionResponse.DifficultyInfo.builder()
                    .difficultyCode(diff)
                    .difficultyName(getDifficultyName(diff))
                    .targetCount(target)
                    .actualCount(actualForReport)
                    .independentItems(independent)
                    .passageGroups(pgCount)
                    .targetPercentage(targetPct)
                    .actualPercentage(actualPct)
                    .build();

            difficultyDistribution.put(diff, info);
        }

        // 3) metadata
        long independentItemsCount = selectedItems.stream().filter(it -> it.getPassageId() == null).count();
        int selectionUnitCount = (int) independentItemsCount + passageGroups.size();

        SmartSelectionResponse.SmartSelectionMetadata metadata = SmartSelectionResponse.SmartSelectionMetadata.builder()
                .requestedCount(requestedCount)
                .actualItemCount(actualCount)
                .selectionUnitCount(selectionUnitCount)
                .passageGroupCount(passageGroups.size())
                .difficultyDistribution(difficultyDistribution)
                .passageGroups(passageGroups)
                .fallbackActions(fallbackActions)
                .build();

        // 4) report
        double accuracy = calculateDistributionAccuracy(targetCounts, actualItemCounts, requestedCount);
        List<String> warnings = generateWarnings(actualCount, requestedCount, fallbackActions);

        SmartSelectionResponse.SmartSelectionReport report = SmartSelectionResponse.SmartSelectionReport.builder()
                .success(actualCount == requestedCount)
                .message(String.format("%d개 문항 선택 완료 (지문 그룹 %d개 포함)", actualCount, passageGroups.size()))
                .warnings(warnings)
                .distributionAccuracy(accuracy)
                .build();

        return SmartSelectionResponse.builder()
                .items(itemResponses)
                .metadata(metadata)
                .report(report)
                .build();
    }

    // ---------------- miscellaneous helpers ----------------
    private double calculateDistributionAccuracy(Map<Long, Integer> targetCounts,
                                                 Map<Long, Integer> actualCounts,
                                                 int totalRequested) {
        if (targetCounts == null || targetCounts.isEmpty() || totalRequested <= 0) return 0.0;

        double totalDeviationPct = 0.0;
        for (Map.Entry<Long, Integer> e : targetCounts.entrySet()) {
            Long diff = e.getKey();
            int target = e.getValue();
            int actual = actualCounts.getOrDefault(diff, 0);

            double targetPct = (double) target / totalRequested;
            double actualPct = totalRequested > 0 ? (double) actual / totalRequested : 0.0;
            totalDeviationPct += Math.abs(targetPct - actualPct);
        }
        return Math.max(0.0, (1.0 - totalDeviationPct) * 100.0);
    }

    private List<String> generateWarnings(int actualCount, int requestedCount,
                                          List<SmartSelectionResponse.FallbackAction> actions) {
        List<String> warnings = new ArrayList<>();
        if (actualCount < requestedCount) {
            warnings.add(String.format("요청된 %d개 중 %d개만 선택 가능", requestedCount, actualCount));
        }
        if (actions != null && !actions.isEmpty()) {
            warnings.add(String.format("%d개의 재분배/대체 동작 발생", actions.size()));
        }
        return warnings;
    }

    private Long getMostCommonDifficulty(List<ItemImageDocument> items) {
        return items.stream().collect(Collectors.groupingBy(ItemImageDocument::getDifficultyCode, Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(3L);
    }

    private String getDifficultyName(Long difficultyCode) {
        return switch (difficultyCode.intValue()) {
            case 1, 2 -> "하";
            case 3 -> "중";
            case 4, 5 -> "상";
            default -> "알 수 없음";
        };
    }

    // --- Placeholder: mapper from ItemImageDocument -> ItemSearchResponse ---
    private ItemSearchResponse convertToItemSearchResponse(ItemImageDocument doc) {
        return ItemSearchResponse.builder()
                .itemId(doc.getItemId())
                .subjectId(doc.getSubjectId())
                .passageId(doc.getPassageId())
                .questionImageUrl(doc.getQuestionUrl())
                .answerImageUrl(doc.getAnswerUrl())
                .explainImageUrl(doc.getExplainUrl())
                .passageImageUrl(doc.getPassageUrl())
                .build();
    }

    // 유사문항 조회 기능
    public List<ItemImageDocument> findSimilarItems(
            long topicChapterId,
            int difficultyCode,
            long passageId,
            List<Long> excludeItemIds,
            int size
    ) throws IOException {
        long subjectId = truncateCode(topicChapterId, 4);
        long largeChapterId = truncateCode(topicChapterId, 6);
        long mediumChapterId = truncateCode(topicChapterId, 8);
        long smallChapterId = truncateCode(topicChapterId, 10);

        BoolQuery.Builder baseBoolQuery = new BoolQuery.Builder();

        if (passageId != -1) {
            baseBoolQuery.must(mn -> mn.term(t -> t
                    .field("passage_id")
                    .value(passageId)
            ));
        } else {
            baseBoolQuery.mustNot(mn -> mn.exists(e -> e
                    .field("passage_id")
            ));
        }

        if (excludeItemIds != null && !excludeItemIds.isEmpty()) {
            baseBoolQuery.mustNot(mn -> mn.terms(t -> t
                    .field("item_id")
                    .terms(tt -> tt.value(
                            excludeItemIds.stream().map(FieldValue::of).collect(Collectors.toList())
                    ))
            ));
        }

        List<FunctionScore> functions = new ArrayList<>();
        if (1 <= difficultyCode && difficultyCode <= 5) {
            int stdDifficultyCode = passageId != -1 ? 1 : difficultyCode;
            for (int diff = 1; diff <= 5; diff++) {
                double weight = 5.0 - Math.abs(diff - stdDifficultyCode);
                final int diffValue = diff;
                functions.add(FunctionScore.of(f -> f
                        .filter(q -> q.term(t -> t.field("difficulty_code").value(diffValue)))
                        .weight(weight)
                ));
            }
        }

        functions.add(buildWeightedFilter("subject_id", subjectId, 15.0));
        functions.add(buildWeightedFilter("large_chapter_id", largeChapterId, 25.0));
        functions.add(buildWeightedFilter("medium_chapter_id", mediumChapterId, 35.0));
        functions.add(buildWeightedFilter("small_chapter_id", smallChapterId, 45.0));
        functions.add(buildWeightedFilter("topic_chapter_id", topicChapterId, 55.0));

        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(INDEX_NAME)
                .query(q -> q.functionScore(fs -> fs
                        .query(qb -> qb.bool(baseBoolQuery.build()))
                        .functions(functions)
                        .scoreMode(FunctionScoreMode.Sum)
                        .boostMode(FunctionBoostMode.Replace)
                ))
                .size(size)
        );

        SearchResponse<ItemImageDocument> response = elasticsearchClient.search(searchRequest, ItemImageDocument.class);

        return response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());
    }

    // 상위 단계 코드 자르기
    private long truncateCode(long code, int length) {
        String codeStr = String.valueOf(code);
        if (codeStr.length() < length) {
            return code;
        }
        return Long.parseLong(codeStr.substring(0, length));
    }

    // Weighted Filter 생성
    private FunctionScore buildWeightedFilter(String fieldName, long value, double weight) {
        return FunctionScore.of(f -> f
                .filter(q -> q.term(t -> t.field(fieldName).value(value)))
                .weight(weight)
        );
    }
}
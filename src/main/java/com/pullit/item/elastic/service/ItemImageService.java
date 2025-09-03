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

        // ===== 최우선 가용성 검사 =====
        SmartSelectionResponse availabilityCheck = checkOverallAvailability(context);
        if (availabilityCheck != null) {
            log.info("전체 가용성 부족으로 조기 종료 - 가용: {}개, 요청: {}개",
                    availabilityCheck.getMetadata().getActualItemCount(), request.getItemCount());
            return availabilityCheck;
        }

        // 충분한 가용 문항이 있을 때만 세부 로직 실행
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

    // ===== 신규: 공용 가용성 검사 메서드 =====
    /**
     * 전체 가용 문항 수를 확인하고, 요청 개수보다 적으면 가능한 모든 문항을 반환
     * @param context 선택 컨텍스트
     * @return 부족한 경우 SmartSelectionResponse, 충분한 경우 null
     */
    private SmartSelectionResponse checkOverallAvailability(SelectionContext context) throws IOException {
        int requestedTotal = context.getRequest().getItemCount();
        int totalAvailable = 0;
        BoolQuery baseBool = context.getBaseQuery();

        // 난이도별 가용 문항 수 집계
        for (Long difficultyCode : context.getTargetCounts().keySet()) {
            BoolQuery.Builder q = cloneFromBase(baseBool);
            q.must(TermQuery.of(t -> t.field("difficulty_code").value(difficultyCode))._toQuery());

            SearchRequest countReq = SearchRequest.of(s -> s.index(INDEX_NAME).size(0).query(q.build()._toQuery()));
            SearchResponse<ItemImageDocument> countResp = elasticsearchClient.search(countReq, ItemImageDocument.class);
            long cnt = countResp.hits().total() != null ? countResp.hits().total().value() : 0L;
            totalAvailable += (int) cnt;
        }

        log.info("전체 가용성 검사: 요청 {}개, 가용 총합 {}개", requestedTotal, totalAvailable);

        // 가용 문항이 충분하면 null 반환 (정상 진행)
        if (totalAvailable > requestedTotal) {
            return null;
        }

        // 가용 문항이 부족하면 모든 가용 문항 반환
        log.warn("가용 문항 부족 - 모든 가용 문항 반환: {}개", totalAvailable);
        return fetchAllAvailableItems(context, totalAvailable);
    }

    // ===== 신규: 모든 가용 문항 조회 메서드 =====
    /**
     * 모든 가용 문항을 조회하여 반환
     */
    private SmartSelectionResponse fetchAllAvailableItems(SelectionContext context, int totalAvailable) throws IOException {
        BoolQuery.Builder allQ = cloneFromBase(context.getBaseQuery());

        SearchRequest fetchAll = SearchRequest.of(s -> s
                .index(INDEX_NAME)
                .size(Math.min(totalAvailable, MAX_SEARCH_SIZE))
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
        int requestedTotal = context.getRequest().getItemCount();
        fallbackActions.add(SmartSelectionResponse.FallbackAction.builder()
                .action("INSUFFICIENT_TOTAL_ITEMS_EARLY_CHECK")
                .count(requestedTotal - items.size())
                .reason("전체 가용 문항수가 요청보다 적음 - 조기 검사에서 모든 가용 문항 반환")
                .build());

        return buildSmartSelectionResponse(items, passageGroups, context.getTargetCounts(), actualItemCounts, fallbackActions, context.getRequest());
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

    // ===== 개선된 독립 문항 선택 로직 =====

    // -------------------- independent selection helpers ----------------------
    private SmartSelectionResponse selectIndependentItemsOnly(SelectionContext context) throws IOException {
        log.info("독립 문항만 선택 시작");

        List<SmartSelectionResponse.FallbackAction> fallbackActions = new ArrayList<>();

        // 개선된 통합 조회 방식 사용
        List<ItemImageDocument> selectedItems = selectIndependentItemsImproved(context, fallbackActions);

        // 최종 중복 제거
        if (context.getRequest().isAvoidDuplicate()) {
            selectedItems = removeDuplicates(selectedItems);
        }

        Map<Long, Integer> actualItemCounts = selectedItems.stream()
                .collect(Collectors.groupingBy(ItemImageDocument::getDifficultyCode, Collectors.summingInt(i -> 1)));

        return buildSmartSelectionResponse(selectedItems, Collections.emptyList(), context.getTargetCounts(), actualItemCounts, fallbackActions, context.getRequest());
    }

    /**
     * 개선된 독립 문항 선택: 통합 조회 후 분배하는 방식
     */
    private List<ItemImageDocument> selectIndependentItemsImproved(SelectionContext context,
                                                                   List<SmartSelectionResponse.FallbackAction> fallbackActions) throws IOException {

        // 1. 모든 독립 문항을 한 번에 조회
        Map<Long, List<ItemImageDocument>> availableByDifficulty = fetchAllAvailableIndependentItems(context);

        List<ItemImageDocument> selected = new ArrayList<>();
        Map<Long, Integer> remainingTargets = new HashMap<>(context.getTargetCounts());

        // 2. 1차: 목표 난이도별 선택
        for (Map.Entry<Long, Integer> entry : remainingTargets.entrySet()) {
            Long difficulty = entry.getKey();
            int needed = entry.getValue();

            List<ItemImageDocument> candidates = availableByDifficulty.getOrDefault(difficulty, new ArrayList<>());
            Collections.shuffle(candidates, random);

            int actualSelected = Math.min(needed, candidates.size());
            if (actualSelected > 0) {
                List<ItemImageDocument> selectedForDifficulty = candidates.subList(0, actualSelected);
                selected.addAll(selectedForDifficulty);

                // 선택된 항목들을 전역 세트에 추가
                selectedForDifficulty.forEach(item -> context.getSelectedItemIds().add(item.getItemId()));

                // 사용된 것들을 후보군에서 제거
                candidates.subList(0, actualSelected).clear();

                log.info("난이도 {} 독립 문항 {}개 선택 (목표: {}개)", difficulty, actualSelected, needed);
            }

            if (actualSelected < needed) {
                fallbackActions.add(SmartSelectionResponse.FallbackAction.builder()
                        .action("INSUFFICIENT_INDEPENDENT_ITEMS")
                        .fromDifficulty(difficulty)
                        .count(needed - actualSelected)
                        .reason(String.format("난이도 %d 독립 문항 부족 (%d개 요청, %d개 선택)", difficulty, needed, actualSelected))
                        .build());
            }
        }

        // 3. 2차: 부족분이 있으면 계층적 보충
        int totalRequested = context.getRequest().getItemCount();
        if (selected.size() < totalRequested) {
            int shortage = totalRequested - selected.size();
            List<ItemImageDocument> additional = fillRemainingWithFallback(context, selected, availableByDifficulty, shortage, fallbackActions);
            selected.addAll(additional);

            log.info("부족분 보충: {}개 추가 선택", additional.size());
        }

        return selected;
    }

    /**
     * 모든 독립 문항을 한 번에 조회하여 난이도별로 분류
     */
    private Map<Long, List<ItemImageDocument>> fetchAllAvailableIndependentItems(SelectionContext context) throws IOException {
        BoolQuery.Builder q = cloneFromBase(context.getBaseQuery());
        q.mustNot(ExistsQuery.of(e -> e.field("passage_id"))._toQuery());

        // 중복 제거
        if (context.getRequest().isAvoidDuplicate() && !context.getSelectedItemIds().isEmpty()) {
            for (Long itemId : context.getSelectedItemIds()) {
                q.mustNot(TermQuery.of(t -> t.field("item_id").value(itemId))._toQuery());
            }
        }

        SearchRequest request = SearchRequest.of(s -> s
                .index(INDEX_NAME)
                .size(MAX_SEARCH_SIZE)
                .query(q.build()._toQuery())
        );

        SearchResponse<ItemImageDocument> response = elasticsearchClient.search(request, ItemImageDocument.class);
        List<ItemImageDocument> allIndependents = response.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.info("전체 독립 문항 조회: {}개", allIndependents.size());

        // 난이도별로 분류하여 반환
        return allIndependents.stream()
                .collect(Collectors.groupingBy(ItemImageDocument::getDifficultyCode));
    }

    /**
     * 계층적 부족분 보충 전략
     */
    private List<ItemImageDocument> fillRemainingWithFallback(SelectionContext context,
                                                              List<ItemImageDocument> currentSelected,
                                                              Map<Long, List<ItemImageDocument>> remainingPool,
                                                              int shortage,
                                                              List<SmartSelectionResponse.FallbackAction> fallbackActions) {
        if (shortage <= 0) return new ArrayList<>();

        List<ItemImageDocument> additional = new ArrayList<>();

        // 전략 1: 부족한 난이도 우선 (가중치 기반)
        Map<Long, Integer> shortageByDifficulty = calculateShortageByDifficulty(context, currentSelected);
        int phase1Target = Math.min(shortage / 2, shortage);
        additional.addAll(selectByWeightedPriority(remainingPool, shortageByDifficulty, phase1Target));

        // 전략 2: 남은 부족분은 균등 분배
        int remainingShortage = shortage - additional.size();
        if (remainingShortage > 0) {
            additional.addAll(selectByEvenDistribution(remainingPool, remainingShortage));
        }

        // 선택된 항목들을 전역 세트에 추가
        additional.forEach(item -> context.getSelectedItemIds().add(item.getItemId()));

        if (additional.size() < shortage) {
            fallbackActions.add(SmartSelectionResponse.FallbackAction.builder()
                    .action("INSUFFICIENT_ADDITIONAL_ITEMS")
                    .count(shortage - additional.size())
                    .reason(String.format("추가 독립문항으로도 부족분 해결 불가 (%d개 요청, %d개 선택)", shortage, additional.size()))
                    .build());
        }

        return additional;
    }

    /**
     * 현재까지 선택된 문항을 기준으로 난이도별 부족분 계산
     */
    private Map<Long, Integer> calculateShortageByDifficulty(SelectionContext context, List<ItemImageDocument> currentSelected) {
        Map<Long, Integer> actualCounts = currentSelected.stream()
                .collect(Collectors.groupingBy(ItemImageDocument::getDifficultyCode, Collectors.summingInt(i -> 1)));

        Map<Long, Integer> shortages = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : context.getTargetCounts().entrySet()) {
            Long difficulty = entry.getKey();
            int target = entry.getValue();
            int actual = actualCounts.getOrDefault(difficulty, 0);
            int shortage = Math.max(0, target - actual);

            if (shortage > 0) {
                shortages.put(difficulty, shortage);
            }
        }

        return shortages;
    }

    /**
     * 가중치 기반 우선순위 선택 (부족분이 큰 난이도부터)
     */
    private List<ItemImageDocument> selectByWeightedPriority(Map<Long, List<ItemImageDocument>> pool,
                                                             Map<Long, Integer> shortageWeights,
                                                             int maxSelect) {
        List<ItemImageDocument> result = new ArrayList<>();

        // 부족분이 큰 난이도부터 우선 선택
        List<Map.Entry<Long, Integer>> sortedShortages = shortageWeights.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());

        for (Map.Entry<Long, Integer> entry : sortedShortages) {
            if (result.size() >= maxSelect) break;

            Long difficulty = entry.getKey();
            int weight = entry.getValue();

            List<ItemImageDocument> candidates = pool.getOrDefault(difficulty, new ArrayList<>());
            Collections.shuffle(candidates, random);

            int selectCount = Math.min(weight, Math.min(candidates.size(), maxSelect - result.size()));
            if (selectCount > 0) {
                result.addAll(candidates.subList(0, selectCount));
                candidates.subList(0, selectCount).clear();

                log.info("가중치 선택 - 난이도 {}: {}개 추가", difficulty, selectCount);
            }
        }

        return result;
    }

    /**
     * 균등 분배 선택 (남은 모든 난이도에서 골고루)
     */
    private List<ItemImageDocument> selectByEvenDistribution(Map<Long, List<ItemImageDocument>> pool, int maxSelect) {
        List<ItemImageDocument> result = new ArrayList<>();

        // 사용 가능한 난이도들
        List<Long> availableDifficulties = pool.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (availableDifficulties.isEmpty()) return result;

        // 라운드 로빈 방식으로 균등 선택
        int roundRobinIndex = 0;
        while (result.size() < maxSelect && !availableDifficulties.isEmpty()) {
            Long difficulty = availableDifficulties.get(roundRobinIndex % availableDifficulties.size());
            List<ItemImageDocument> candidates = pool.get(difficulty);

            if (candidates != null && !candidates.isEmpty()) {
                Collections.shuffle(candidates, random);
                result.add(candidates.remove(0));

                if (candidates.isEmpty()) {
                    availableDifficulties.remove(difficulty);
                    if (availableDifficulties.isEmpty()) break;
                    roundRobinIndex = roundRobinIndex % availableDifficulties.size();
                } else {
                    roundRobinIndex = (roundRobinIndex + 1) % availableDifficulties.size();
                }
            } else {
                availableDifficulties.remove(difficulty);
                if (availableDifficulties.isEmpty()) break;
                roundRobinIndex = roundRobinIndex % availableDifficulties.size();
            }
        }

        log.info("균등 분배 선택: {}개", result.size());
        return result;
    }

    // ----------------------- main flow: passage groups -----------------------
    private SmartSelectionResponse selectWithPassageGroupsBalanced(SelectionContext context) throws IOException {
        log.info("지문 포함 선택 시작");

        List<ItemImageDocument> selectedItems = new ArrayList<>();
        List<SmartSelectionResponse.PassageGroupInfo> passageGroups = new ArrayList<>();
        List<SmartSelectionResponse.FallbackAction> fallbackActions = new ArrayList<>();

        Map<Long, Integer> remainingCounts = new HashMap<>(context.getTargetCounts());

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

        // 3) Fill with improved independent items
        List<ItemImageDocument> independents = selectIndependentItemsForPassageMode(context, selectedItems, fallbackActions);
        selectedItems.addAll(independents);

        // 4) 부족분 체크는 이미 selectIndependentItemsForPassageMode에서 처리됨

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

    /**
     * 지문 모드에서의 독립 문항 선택 (이미 선택된 지문 고려)
     */
    private List<ItemImageDocument> selectIndependentItemsForPassageMode(SelectionContext context,
                                                                         List<ItemImageDocument> alreadySelected,
                                                                         List<SmartSelectionResponse.FallbackAction> fallbackActions) throws IOException {

        int requestedTotal = context.getRequest().getItemCount();
        int alreadySelectedCount = alreadySelected.size();

        if (alreadySelectedCount >= requestedTotal) {
            log.info("이미 요청 개수({})를 충족 - 추가 독립 문항 선택 불필요", requestedTotal);
            return new ArrayList<>();
        }

        int remainingNeeded = requestedTotal - alreadySelectedCount;
        log.info("추가 필요 문항 수: {}개 (전체 요청: {}, 이미 선택: {})",
                remainingNeeded, requestedTotal, alreadySelectedCount);

        // 이미 선택된 문항 기준으로 난이도별 남은 목표 계산
        Map<Long, Integer> remainingCounts = recalculateRemainingCounts(context.getTargetCounts(), alreadySelected);

        // 난이도별 목표가 있는 경우에만 해당 난이도 우선 선택
        if (!remainingCounts.isEmpty()) {
            List<ItemImageDocument> targetedSelection = selectTargetedIndependentItems(
                    context, remainingCounts, remainingNeeded, fallbackActions);

            if (targetedSelection.size() >= remainingNeeded) {
                // 필요한 만큼 선택되었으면 제한하여 반환
                List<ItemImageDocument> limited = targetedSelection.subList(0, remainingNeeded);
                log.info("목표 기반 선택 완료: {}개", limited.size());
                return limited;
            } else {
                // 목표 기반 선택으로 부족하면 추가 선택
                int additionalNeeded = remainingNeeded - targetedSelection.size();
                List<ItemImageDocument> additional = selectAdditionalItemsAnyDifficulty(
                        context, additionalNeeded, fallbackActions);

                targetedSelection.addAll(additional);
                log.info("목표 + 추가 선택 완료: {}개", targetedSelection.size());
                return targetedSelection;
            }
        } else {
            // 모든 난이도 목표 달성 시 전체 개수 부족분만 채우기
            log.info("모든 난이도 목표 달성 - 전체 개수 부족분 {}개만 추가 선택", remainingNeeded);
            return selectAdditionalItemsAnyDifficulty(context, remainingNeeded, fallbackActions);
        }
    }

    /**
     * 목표 난이도 기반 독립 문항 선택 (전체 개수 제한 적용)
     */
    private List<ItemImageDocument> selectTargetedIndependentItems(SelectionContext context,
                                                                   Map<Long, Integer> remainingCounts,
                                                                   int maxSelect,
                                                                   List<SmartSelectionResponse.FallbackAction> fallbackActions) throws IOException {

        // 모든 독립 문항을 한 번에 조회
        Map<Long, List<ItemImageDocument>> availableByDifficulty = fetchAllAvailableIndependentItems(context);

        List<ItemImageDocument> selected = new ArrayList<>();

        // 목표 난이도별 선택 (전체 제한 적용)
        for (Map.Entry<Long, Integer> entry : remainingCounts.entrySet()) {
            if (selected.size() >= maxSelect) break; // 전체 제한 체크

            Long difficulty = entry.getKey();
            int needed = entry.getValue();
            int canSelect = Math.min(needed, maxSelect - selected.size()); // 남은 선택 가능 수량

            List<ItemImageDocument> candidates = availableByDifficulty.getOrDefault(difficulty, new ArrayList<>());
            Collections.shuffle(candidates, random);

            int actualSelected = Math.min(canSelect, candidates.size());
            if (actualSelected > 0) {
                List<ItemImageDocument> selectedForDifficulty = candidates.subList(0, actualSelected);
                selected.addAll(selectedForDifficulty);

                // 선택된 항목들을 전역 세트에 추가
                selectedForDifficulty.forEach(item -> context.getSelectedItemIds().add(item.getItemId()));

                // 사용된 것들을 후보군에서 제거
                candidates.subList(0, actualSelected).clear();

                log.info("난이도 {} 독립 문항 {}개 선택 (목표: {}개, 제한: {}개)",
                        difficulty, actualSelected, needed, canSelect);
            }

            if (actualSelected < canSelect) {
                fallbackActions.add(SmartSelectionResponse.FallbackAction.builder()
                        .action("INSUFFICIENT_TARGETED_INDEPENDENT_ITEMS")
                        .fromDifficulty(difficulty)
                        .count(canSelect - actualSelected)
                        .reason(String.format("난이도 %d 독립 문항 부족 (%d개 요청, %d개 선택)",
                                difficulty, canSelect, actualSelected))
                        .build());
            }
        }

        return selected;
    }

    /**
     * 난이도 무시하고 부족분만큼 추가 선택
     */
    private List<ItemImageDocument> selectAdditionalItemsAnyDifficulty(SelectionContext context,
                                                                       int shortage,
                                                                       List<SmartSelectionResponse.FallbackAction> fallbackActions) throws IOException {
        if (shortage <= 0) return new ArrayList<>();

        log.info("추가 문항 선택 시작 - 필요 수량: {}개", shortage);

        // 모든 독립 문항 조회 (난이도 조건 없음)
        Map<Long, List<ItemImageDocument>> allAvailable = fetchAllAvailableIndependentItems(context);

        // 모든 난이도를 하나의 풀로 합치기
        List<ItemImageDocument> combinedPool = new ArrayList<>();
        allAvailable.values().forEach(combinedPool::addAll);

        Collections.shuffle(combinedPool, random);

        int actualSelected = Math.min(shortage, combinedPool.size());
        List<ItemImageDocument> selected = new ArrayList<>();

        if (actualSelected > 0) {
            selected = combinedPool.subList(0, actualSelected);

            // 전역 세트에 추가
            selected.forEach(item -> context.getSelectedItemIds().add(item.getItemId()));
        }

        if (actualSelected < shortage) {
            fallbackActions.add(SmartSelectionResponse.FallbackAction.builder()
                    .action("INSUFFICIENT_TOTAL_ITEMS_FINAL")
                    .count(shortage - actualSelected)
                    .reason(String.format("최종 부족분 %d개 - 더 이상 선택할 문항 없음", shortage - actualSelected))
                    .build());
        }

        log.info("추가 문항 선택 완료: {}개 (요청: {}개)", actualSelected, shortage);
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

        // 먼저 난이도별 균형 선택 시도 (Greedy 방식)
        List<PassageGroupCandidate> greedySelection = tryGreedyPassageSelection(candidates, targetCounts, context);
        if (!greedySelection.isEmpty()) {
            log.info("Greedy 지문 선택 성공: {}개 그룹", greedySelection.size());
            return greedySelection;
        }

        // Greedy 선택 실패시 기존 DP 로직 사용
        log.info("Greedy 선택 실패 - DP 알고리즘 사용");
        return selectPassageGroupsWithOriginalDP(candidates, targetCounts, context);
    }

    // 개선된 네이밍: Greedy 알고리즘임을 명시
    private List<PassageGroupCandidate> tryGreedyPassageSelection(List<PassageGroupCandidate> candidates,
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

        // Greedy 우선순위: 부족한 난이도부터 (하 → 상 → 중 순서로)
        List<Long> greedyPriority = Arrays.asList(2L, 4L, 3L);

        int totalSelectedItems = 0;

        for (Long difficulty : greedyPriority) {
            if (totalSelectedItems >= maxPassageItems) break;

            List<PassageGroupCandidate> diffCandidates = byDifficulty.getOrDefault(difficulty, new ArrayList<>());
            if (diffCandidates.isEmpty()) continue;

            int targetForDiff = remainingTargets.getOrDefault(difficulty, 0);
            if (targetForDiff <= 0) continue;

            // Greedy: 난이도별 지문 그룹을 효율성으로 정렬 (작은 그룹부터)
            diffCandidates.sort(Comparator.comparingInt(PassageGroupCandidate::getItemCount));

            int currentSelected = 0;
            for (PassageGroupCandidate candidate : diffCandidates) {
                if (totalSelectedItems >= maxPassageItems) break;
                if (currentSelected >= targetForDiff) break;

                int candidateSize = candidate.getItemCount();

                // Greedy 조건: 목표 개수를 초과하지 않고, 전체 제한도 넘지 않는 경우만 즉시 선택
                if (currentSelected + candidateSize <= targetForDiff &&
                        totalSelectedItems + candidateSize <= maxPassageItems) {

                    selected.add(candidate);
                    currentSelected += candidateSize;
                    totalSelectedItems += candidateSize;

                    log.info("Greedy 선택 - 난이도 {}: 지문 {} ({}개 문항)",
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
                        take[i][d2][d3][d4] = false;

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

        // 최적해 위치 찾기 (목표치와 정확히 일치하지 않아도 OK)
        int bestVal = -1;
        int bestD2 = 0, bestD3 = 0, bestD4 = 0;
        for (int d2 = 0; d2 <= maxD2; d2++) {
            for (int d3 = 0; d3 <= maxD3; d3++) {
                for (int d4 = 0; d4 <= maxD4; d4++) {
                    if (dp[n][d2][d3][d4] > bestVal) {
                        bestVal = dp[n][d2][d3][d4];
                        bestD2 = d2; bestD3 = d3; bestD4 = d4;
                    }
                }
            }
        }

        List<PassageGroupCandidate> selected = new ArrayList<>();
        int d2 = bestD2, d3 = bestD3, d4 = bestD4;
        for (int i = n; i >= 1; i--) {
            if (take[i][d2][d3][d4]) {
                PassageGroupCandidate c = validCandidates.get(i - 1);
                selected.add(c);
                if (c.getDifficultyCode() == 2L && d2 >= c.getItemCount()) {
                    d2 -= c.getItemCount();
                } else if (c.getDifficultyCode() == 3L && d3 >= c.getItemCount()) {
                    d3 -= c.getItemCount();
                } else if (c.getDifficultyCode() == 4L && d4 >= c.getItemCount()) {
                    d4 -= c.getItemCount();
                }
            }
        }
        return selected;
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

        // 가중치 함수 생성
        List<FunctionScore> functions = new ArrayList<>();
        // 챕터별 가중치 설정
        functions.add(buildWeightedFilter("subject_id", subjectId, 15.0));
        functions.add(buildWeightedFilter("large_chapter_id", largeChapterId, 25.0));
        functions.add(buildWeightedFilter("medium_chapter_id", mediumChapterId, 35.0));
        functions.add(buildWeightedFilter("small_chapter_id", smallChapterId, 45.0));
        functions.add(buildWeightedFilter("topic_chapter_id", topicChapterId, 55.0));
        // 난이도별 가중치 설정
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

        // 가중치합 기반 유사문항 조회
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
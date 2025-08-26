package com.pullit.item.service;

import com.pullit.common.annotation.LoggingTrace;
import com.pullit.item.config.DifficultyDistribution;
import com.pullit.item.dao.ItemHtmlDataRepository;
import com.pullit.item.dao.ItemImageDataRepository;
import com.pullit.item.dao.ItemMetadataRepository;
import com.pullit.item.dto.request.ItemSearchRequest;
import com.pullit.item.dto.request.SmartSelectionRequest;
import com.pullit.item.dto.response.ItemSearchResponse;
import com.pullit.item.dto.response.SmartSelectionResponse;
import com.pullit.item.entity.ItemHtmlData;
import com.pullit.item.entity.ItemImageData;
import com.pullit.item.entity.ItemMetadata;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemSearchServiceImpl implements ItemSearchService {

    private final ItemHtmlDataRepository itemHtmlDataRepository;
    private final ItemImageDataRepository itemImageDataRepository;
    private final ItemMetadataRepository itemMetadataRepository;

    @Override
    @LoggingTrace(level = LoggingTrace.LogLevel.INFO, logExecutionTime = true, logParameters = true)
    public Page<ItemSearchResponse> searchItems(ItemSearchRequest request) {
        log.info("[인덱스 없음] 문항 검색 시작: {}",request);

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        Page<ItemMetadata> itemPage = itemMetadataRepository.searchItems(request, pageable);
        log.info("검색 결과: {} 건", itemPage.getTotalElements());

        return itemPage.map(this::convertToResponse);
    }

    @Override
    public ItemSearchResponse getItemDetail(Long itemId) {
        ItemMetadata metadata = itemMetadataRepository.findByItemId(itemId)
                .orElseThrow(() -> new EntityNotFoundException("문항을 찾을 수 없습니다: " + itemId));

        return convertToResponse(metadata);
    }

    @Override
    // @Cacheable(value = "chapterItemCounts", key = "#subjectId + '-' + #chapterIds.hashCode()") // 캐시 비활성화 - 성능 측정
    @LoggingTrace(level = LoggingTrace.LogLevel.INFO, logExecutionTime = true)
    public Map<Long, Long> getItemCountsByChapters(Long subjectId, List<Long> chapterIds) {
        log.info("[인덱스 없음] 챕터별 문항 수 집계: subjectId={}, chapterIds={}", subjectId, chapterIds);
        return itemMetadataRepository.countItemsByChapters(subjectId, chapterIds);
    }

    @Override
    // @Cacheable(value = "difficultyItemCounts", key = "#subjectId") // 캐시 비활성화 - 성능 측정
    @LoggingTrace(level = LoggingTrace.LogLevel.INFO, logExecutionTime = true)
    public Map<Long, Long> getItemCountsByDifficulty(Long subjectId) {
        log.info("[인덱스 없음] 난이도별 문항 수 집계: subjectId={}", subjectId);
        return itemMetadataRepository.countItemsByDifficulty(subjectId);
    }

    @Override
    // @Cacheable(value = "questionFormItemCounts", key = "#subjectId") // 캐시 비활성화 - 성능 측정
    @LoggingTrace(level = LoggingTrace.LogLevel.INFO, logExecutionTime = true)
    public Map<Long, Long> getItemCountsByQuestionForm(Long subjectId) {
        log.info("[인덱스 없음] 문제 형식별 문항 수 집계: subjectId={}", subjectId);
        return itemMetadataRepository.countItemsByQuestionForm(subjectId);
    }

    @Override
    public Map<Long, Long> getItemCountsBySubjects(List<Long> subjectIds) {
        return itemMetadataRepository.countItemsBySubjects(subjectIds);
    }

    @Override
    public List<ItemSearchResponse> getItemsByPassage(Long passageId) {
        List<ItemMetadata> items = itemMetadataRepository.findByPassageId(passageId);

        return items.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemSearchResponse> getItemsByIds(List<Long> itemIds) {
        List<ItemMetadata> items = itemMetadataRepository.findAllById(itemIds);

        return items.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Entity를 Response DTO로 변환
     */
    private ItemSearchResponse convertToResponse(ItemMetadata metadata) {
        ItemSearchResponse.ItemSearchResponseBuilder builder = ItemSearchResponse.builder()
                .itemId(metadata.getItemId())
                .subjectId(metadata.getSubject() != null ? metadata.getSubject().getSubjectId() : null)
                .subjectName(metadata.getSubject() != null ? metadata.getSubject().getSubjectName() : null)
                .hasImageData(metadata.getHasImageData())
                .hasHtmlData(metadata.getHasHtmlData())
                .questionForm(metadata.getQuestionForm())
                .difficulty(metadata.getDifficulty())
                .chapterHierarchy(metadata.getChapterHierarchy())
                .passageId(metadata.getPassageId())
                .createdDate(metadata.getCreatedDate())
                .updatedDate(metadata.getUpdatedDate());

        // 이미지 데이터 처리
        if (metadata.getHasImageData() && metadata.getImageData() != null) {
            ItemImageData imageData = metadata.getImageData();
            builder.questionImageUrl(imageData.getQuestionUrl())
                    .answerImageUrl(imageData.getAnswerUrl())
                    .explainImageUrl(imageData.getExplainUrl())
                    .passageImageUrl(imageData.getPassageUrl());
        }

        // HTML 데이터 처리 (필요시)
        if (metadata.getHasHtmlData() && metadata.getHtmlData() != null) {
            ItemHtmlData htmlData = metadata.getHtmlData();
            builder.questionHtml(htmlData.getQuestionHtml())
                    .answerHtml(htmlData.getAnswerHtml())
                    .explainHtml(htmlData.getExplainHtml())
                    .passageHtml(htmlData.getPassageHtml())
                    .choice1Html(htmlData.getChoice1Html())
                    .choice2Html(htmlData.getChoice2Html())
                    .choice3Html(htmlData.getChoice3Html())
                    .choice4Html(htmlData.getChoice4Html())
                    .choice5Html(htmlData.getChoice5Html());
        }

        return builder.build();
    }

    @Override
    @Transactional(readOnly = true)
    public SmartSelectionResponse smartRandomSelection(SmartSelectionRequest request) {
        log.info("스마트 문항 선택 시작 -교과서 :{}, 문항수 {}, 난이도 {}",request.getSubjectId(), request.getItemCount(),request.getDifficulty());

        DifficultyDistribution distribution = DifficultyDistribution.fromCode(request.getDifficulty());

        Map<Long, Integer> targetCounts = calculateTargetCounts(distribution, request.getItemCount());

        Map<Long, Map<String,Long>> availableCounts = new HashMap<>();
        for(Long difficulty : Arrays.asList(1L,2L,3L)) {
            availableCounts.put(difficulty,
                    itemMetadataRepository.countSelectionUnitsByDifficulty(
                            request.getSubjectId(), request.getChapters(), difficulty));
        }
        Map<Long, Integer> adjustedCounts = adjustDistribution(targetCounts, availableCounts);

        // 문항 선택 수행
        List<ItemMetadata> selectedItems = new ArrayList<>();
        List<SmartSelectionResponse.PassageGroupInfo> passageGroups = new ArrayList<>();
        List<SmartSelectionResponse.FallbackAction> fallbackActions = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : adjustedCounts.entrySet()) {
            Long difficulty = entry.getKey();
            int count = entry.getValue();

            if (count <= 0) continue;

            // 독립 문항과 지문 그룹의 비율 결정 (7:3 정도로 설정)
            int independentTarget = (int)(count * 0.7);
            int passageGroupTarget = count - independentTarget;

            // 독립 문항 선택
            List<ItemMetadata> independentItems =
                    itemMetadataRepository.findRandomItemsWithPassageGrouping(
                            request.getSubjectId(), request.getChapters(),
                            difficulty, independentTarget, true);

            selectedItems.addAll(independentItems);

            // 지문 그룹 선택
            if (request.isIncludePassage() && passageGroupTarget > 0) {
                List<ItemMetadata> passageRepresentatives =
                        itemMetadataRepository.findRandomItemsWithPassageGrouping(
                                request.getSubjectId(), request.getChapters(),
                                difficulty, passageGroupTarget, false);

                // 선택된 지문의 모든 연결 문항 가져오기
                if (!passageRepresentatives.isEmpty()) {
                    List<Long> passageIds = passageRepresentatives.stream()
                            .map(ItemMetadata::getPassageId)
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.toList());

                    List<ItemMetadata> passageItems =
                            itemMetadataRepository.findItemsByPassageIds(passageIds);

                    // 지문 그룹 정보 생성
                    Map<Long, List<ItemMetadata>> passageMap = passageItems.stream()
                            .collect(Collectors.groupingBy(ItemMetadata::getPassageId));

                    for (Map.Entry<Long, List<ItemMetadata>> passageEntry : passageMap.entrySet()) {
                        SmartSelectionResponse.PassageGroupInfo groupInfo =
                                SmartSelectionResponse.PassageGroupInfo.builder()
                                        .passageId(passageEntry.getKey())
                                        .itemCount(passageEntry.getValue().size())
                                        .representativeDifficulty(difficulty)
                                        .itemIds(passageEntry.getValue().stream()
                                                .map(ItemMetadata::getItemId)
                                                .collect(Collectors.toList()))
                                        .build();
                        passageGroups.add(groupInfo);
                    }

                    selectedItems.addAll(passageItems);
                }
            }

            // 부족한 경우 fallback 기록
            int actualCount = independentItems.size() +
                    (request.isIncludePassage() ? passageGroups.size() : 0);

            if (actualCount < count) {
                log.warn("난이도 {}에서 목표 {}개 중 {}개만 선택", difficulty, count, actualCount);
            }
        }

        // Fallback 액션 기록
        recordFallbackActions(targetCounts, adjustedCounts, fallbackActions);

        // 응답 생성
        return buildSmartSelectionResponse(
                selectedItems, passageGroups, targetCounts,
                adjustedCounts, fallbackActions, request);
    }

    private Map<Long, Integer> calculateTargetCounts(DifficultyDistribution distribution, int totalCount) {
        Map<Long, Integer> targets = new HashMap<>();
        Map<Long, Double> dist = distribution.getDistribution();

        int allocated = 0;
        for(Map.Entry<Long, Double> entry : dist.entrySet()) {
            int count= (int) Math.round(totalCount * entry.getValue());
            targets.put(entry.getKey(), count);
            allocated += count;
        }
        if(allocated !=totalCount) {
            Long maxDifficulty = dist.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                   .orElse(2L);
        targets.put(maxDifficulty, targets.get(maxDifficulty)+(totalCount-allocated));
        }
        return targets;
    }

    private Map<Long, Integer> adjustDistribution(
            Map<Long, Integer> targetCounts,
            Map<Long, Map<String, Long>> availableCounts) {

        Map<Long, Integer> adjusted = new HashMap<>(targetCounts);
        List<Long> difficulties = Arrays.asList(1L, 2L, 3L);

        // 각 난이도별로 부족한 수량 계산
        Map<Long, Integer> deficits = new HashMap<>();
        for (Long difficulty : difficulties) {
            int target = targetCounts.getOrDefault(difficulty, 0);
            long available = availableCounts.get(difficulty).get("total");

            if (target > available) {
                int deficit = target - (int)available;
                deficits.put(difficulty, deficit);
                adjusted.put(difficulty, (int)available);
            }
        }

        // 부족한 수량을 다른 난이도에서 재분배
        for (Map.Entry<Long, Integer> deficitEntry : deficits.entrySet()) {
            Long deficitDifficulty = deficitEntry.getKey();
            int deficit = deficitEntry.getValue();

            // 인접 난이도 우선 순위 설정
            List<Long> redistributionOrder = getRedistributionOrder(deficitDifficulty);

            for (Long targetDifficulty : redistributionOrder) {
                if (deficit <= 0) break;

                long available = availableCounts.get(targetDifficulty).get("total");
                int current = adjusted.get(targetDifficulty);
                int surplus = (int)available - current;

                if (surplus > 0) {
                    int redistribution = Math.min(surplus, deficit);
                    adjusted.put(targetDifficulty, current + redistribution);
                    deficit -= redistribution;

                    log.info("난이도 {}에서 {}로 {}개 재분배",
                            deficitDifficulty, targetDifficulty, redistribution);
                }
            }
        }

        return adjusted;
    }

    private List<Long> getRedistributionOrder(Long difficulty) {
        // 인접 난이도 우선 재분배
        if (difficulty == 1L) return Arrays.asList(2L, 3L);
        if (difficulty == 2L) return Arrays.asList(1L, 3L);
        if (difficulty == 3L) return Arrays.asList(2L, 1L);
        return Arrays.asList(2L, 1L, 3L);
    }

    private void recordFallbackActions(
            Map<Long, Integer> targetCounts,
            Map<Long, Integer> adjustedCounts,
            List<SmartSelectionResponse.FallbackAction> actions) {

        for (Long difficulty : targetCounts.keySet()) {
            int target = targetCounts.get(difficulty);
            int adjusted = adjustedCounts.getOrDefault(difficulty, 0);

            if (target != adjusted) {
                SmartSelectionResponse.FallbackAction action =
                        SmartSelectionResponse.FallbackAction.builder()
                                .action(target > adjusted ? "REDUCED" : "INCREASED")
                                .fromDifficulty(difficulty)
                                .toDifficulty(difficulty)
                                .count(Math.abs(target - adjusted))
                                .reason(target > adjusted ? "문항 부족" : "재분배")
                                .build();
                actions.add(action);
            }
        }
    }

    private SmartSelectionResponse buildSmartSelectionResponse(
            List<ItemMetadata> selectedItems,
            List<SmartSelectionResponse.PassageGroupInfo> passageGroups,
            Map<Long, Integer> targetCounts,
            Map<Long, Integer> adjustedCounts,
            List<SmartSelectionResponse.FallbackAction> fallbackActions,
            SmartSelectionRequest request) {

        // 문항을 ItemSearchResponse로 변환
        List<ItemSearchResponse> itemResponses = selectedItems.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        // 난이도별 분포 정보 생성
        Map<Long, SmartSelectionResponse.DifficultyInfo> difficultyDistribution =
                new HashMap<>();

        for (Long difficulty : Arrays.asList(1L, 2L, 3L)) {
            int independentCount = (int) selectedItems.stream()
                    .filter(item -> item.getDifficulty().getCode().equals(difficulty))
                    .filter(item -> item.getPassageId() == null)
                    .count();

            int passageGroupCount = (int) passageGroups.stream()
                    .filter(group -> group.getRepresentativeDifficulty().equals(difficulty))
                    .count();

            SmartSelectionResponse.DifficultyInfo info =
                    SmartSelectionResponse.DifficultyInfo.builder()
                            .difficultyCode(difficulty)
                            .difficultyName(getDifficultyName(difficulty))
                            .targetCount(targetCounts.getOrDefault(difficulty, 0))
                            .actualCount(independentCount + passageGroupCount)
                            .independentItems(independentCount)
                            .passageGroups(passageGroupCount)
                            .targetPercentage(targetCounts.getOrDefault(difficulty, 0) * 100.0 / request.getItemCount())
                            .actualPercentage((independentCount + passageGroupCount) * 100.0 / request.getItemCount())
                            .build();

            difficultyDistribution.put(difficulty, info);
        }

        // 메타데이터 생성
        SmartSelectionResponse.SmartSelectionMetadata metadata =
                SmartSelectionResponse.SmartSelectionMetadata.builder()
                        .requestedCount(request.getItemCount())
                        .actualItemCount(selectedItems.size())
                        .selectionUnitCount(selectedItems.size() -
                                passageGroups.stream().mapToInt(g -> g.getItemCount() - 1).sum())
                        .passageGroupCount(passageGroups.size())
                        .difficultyDistribution(difficultyDistribution)
                        .passageGroups(passageGroups)
                        .fallbackActions(fallbackActions)
                        .build();

        // 리포트 생성
        double accuracy = calculateDistributionAccuracy(targetCounts, adjustedCounts, request.getItemCount());
        List<String> warnings = generateWarnings(selectedItems.size(), request.getItemCount(), fallbackActions);

        SmartSelectionResponse.SmartSelectionReport report =
                SmartSelectionResponse.SmartSelectionReport.builder()
                        .success(selectedItems.size() > 0)
                        .message(String.format("%d개 문항 선택 완료 (지문 그룹 %d개 포함)",
                                selectedItems.size(), passageGroups.size()))
                        .warnings(warnings)
                        .distributionAccuracy(accuracy)
                        .build();

        return SmartSelectionResponse.builder()
                .items(itemResponses)
                .metadata(metadata)
                .report(report)
                .build();
    }

    private String getDifficultyName(Long difficultyCode) {
        switch (difficultyCode.intValue()) {
            case 1: return "하";
            case 2: return "중";
            case 3: return "상";
            default: return "알 수 없음";
        }
    }

    private double calculateDistributionAccuracy(
            Map<Long, Integer> targetCounts,
            Map<Long, Integer> actualCounts,
            int totalCount) {

        double totalDeviation = 0;
        for (Long difficulty : targetCounts.keySet()) {
            int target = targetCounts.get(difficulty);
            int actual = actualCounts.getOrDefault(difficulty, 0);
            double targetPercentage = target * 100.0 / totalCount;
            double actualPercentage = actual * 100.0 / totalCount;
            totalDeviation += Math.abs(targetPercentage - actualPercentage);
        }

        return Math.max(0, 100 - totalDeviation);
    }

    private List<String> generateWarnings(int actualCount, int requestedCount,
                                          List<SmartSelectionResponse.FallbackAction> actions) {
        List<String> warnings = new ArrayList<>();

        if (actualCount < requestedCount) {
            warnings.add(String.format("요청된 %d개 중 %d개만 선택 가능",
                    requestedCount, actualCount));
        }

        if (!actions.isEmpty()) {
            warnings.add(String.format("%d개의 난이도 재분배 발생", actions.size()));
        }

        return warnings;
    }

}

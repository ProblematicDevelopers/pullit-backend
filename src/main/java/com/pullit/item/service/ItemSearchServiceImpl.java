package com.pullit.item.service;

import com.pullit.common.annotation.LoggingTrace;
import com.pullit.common.annotation.RedisCacheable;
import com.pullit.common.annotation.RedisCacheEvict;
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
    @RedisCacheable(
        key = "'item:search:' + " +
              "(#request.keyword != null ? #request.keyword : 'none') + ':' + " +
              "(#request.subjectId != null ? #request.subjectId : 'all') + ':' + " +
              "(#request.chapterId != null ? #request.chapterId : 'all') + ':' + " +
              "(#request.difficulty != null ? #request.difficulty : 'all') + ':' + " +
              "(#request.questionForm != null ? #request.questionForm : 'all')",
        ttl = 30,  // 30분 TTL
        condition = "#request != null"
    )
    @LoggingTrace(level = LoggingTrace.LogLevel.INFO, logExecutionTime = true, logParameters = true)
    public Page<ItemSearchResponse> searchItems(ItemSearchRequest request) {
        log.info("[인덱스 없음] 문항 검색 시작: {}",request);

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        Page<ItemMetadata> itemPage = itemMetadataRepository.searchItems(request, pageable);
        log.info("검색 결과: {} 건", itemPage.getTotalElements());

        return itemPage.map(this::convertToResponse);
    }

    @Override
    @RedisCacheable(
        key = "'item:detail:' + #itemId",
        ttl = 60,  // 1시간 TTL
        timeUnit = java.util.concurrent.TimeUnit.MINUTES,
        condition = "#itemId != null"
    )
    public ItemSearchResponse getItemDetail(Long itemId) {
        ItemMetadata metadata = itemMetadataRepository.findByItemId(itemId)
                .orElseThrow(() -> new EntityNotFoundException("문항을 찾을 수 없습니다: " + itemId));

        return convertToResponse(metadata);
    }

    @Override
    @RedisCacheable(
        key = "'item:count:chapters:' + #subjectId + ':' + T(java.lang.String).join(',', #chapterIds)",
        ttl = 60,  // 1시간 TTL
        timeUnit = java.util.concurrent.TimeUnit.MINUTES,
        condition = "#subjectId != null && #chapterIds != null"
    )
    @LoggingTrace(level = LoggingTrace.LogLevel.INFO, logExecutionTime = true)
    public Map<Long, Long> getItemCountsByChapters(Long subjectId, List<Long> chapterIds) {
        log.info("[인덱스 없음] 챕터별 문항 수 집계: subjectId={}, chapterIds={}", subjectId, chapterIds);
        return itemMetadataRepository.countItemsByChapters(subjectId, chapterIds);
    }

    @Override
    @RedisCacheable(
        key = "'item:count:difficulty:' + #subjectId",
        ttl = 60,  // 1시간 TTL
        timeUnit = java.util.concurrent.TimeUnit.MINUTES,
        condition = "#subjectId != null"
    )
    @LoggingTrace(level = LoggingTrace.LogLevel.INFO, logExecutionTime = true)
    public Map<Long, Long> getItemCountsByDifficulty(Long subjectId) {
        log.info("[인덱스 없음] 난이도별 문항 수 집계: subjectId={}", subjectId);
        return itemMetadataRepository.countItemsByDifficulty(subjectId);
    }

    @Override
    @RedisCacheable(
        key = "'item:count:questionForm:' + #subjectId",
        ttl = 60,  // 1시간 TTL
        timeUnit = java.util.concurrent.TimeUnit.MINUTES,
        condition = "#subjectId != null"
    )
    @LoggingTrace(level = LoggingTrace.LogLevel.INFO, logExecutionTime = true)
    public Map<Long, Long> getItemCountsByQuestionForm(Long subjectId) {
        log.info("[인덱스 없음] 문제 형식별 문항 수 집계: subjectId={}", subjectId);
        return itemMetadataRepository.countItemsByQuestionForm(subjectId);
    }

    @Override
    @RedisCacheable(
        key = "'item:count:subjects:' + T(java.lang.String).join(',', #subjectIds)",
        ttl = 60,  // 1시간 TTL
        timeUnit = java.util.concurrent.TimeUnit.MINUTES,
        condition = "#subjectIds != null && !#subjectIds.isEmpty()"
    )
    public Map<Long, Long> getItemCountsBySubjects(List<Long> subjectIds) {
        return itemMetadataRepository.countItemsBySubjects(subjectIds);
    }

    @Override
    @RedisCacheable(
        key = "'item:byPassage:' + #passageId",
        ttl = 60,  // 1시간 TTL
        timeUnit = java.util.concurrent.TimeUnit.MINUTES,
        condition = "#passageId != null"
    )
    public List<ItemSearchResponse> getItemsByPassage(Long passageId) {
        List<ItemMetadata> items = itemMetadataRepository.findByPassageId(passageId);

        return items.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @RedisCacheable(
        key = "'item:byIds:' + T(java.lang.String).join(',', #itemIds)",
        ttl = 30,  // 30분 TTL
        condition = "#itemIds != null && !#itemIds.isEmpty()"
    )
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

        // 가용한 문항 수 조회
        Map<Long, Map<String,Long>> availableCounts = new HashMap<>();
        for(Long difficulty : Arrays.asList(1L,2L,3L)) {
            availableCounts.put(difficulty,
                    itemMetadataRepository.countSelectionUnitsByDifficulty(
                            request.getSubjectId(), request.getChapters(), difficulty));
        }
        
        // 초기 분배 조정
        Map<Long, Integer> adjustedCounts = adjustDistribution(targetCounts, availableCounts);

        // 결과 저장용 변수들
        List<ItemMetadata> selectedItems = new ArrayList<>();
        List<SmartSelectionResponse.PassageGroupInfo> passageGroups = new ArrayList<>();
        List<SmartSelectionResponse.FallbackAction> fallbackActions = new ArrayList<>();
        Set<Long> selectedItemIds = new HashSet<>();
        
        // 요청된 정확한 개수 추적
        final int targetItemCount = request.getItemCount();
        int remainingNeeded = targetItemCount;
        
        log.info("목표 문항 수: {}, 난이도별 분배: {}", targetItemCount, adjustedCounts);

        // Phase 1: 난이도별 목표에 따른 선택
        for (Map.Entry<Long, Integer> entry : adjustedCounts.entrySet()) {
            if (remainingNeeded <= 0) break;
            
            Long difficulty = entry.getKey();
            int targetForDifficulty = Math.min(entry.getValue(), remainingNeeded);
            
            if (targetForDifficulty <= 0) continue;
            
            log.info("난이도 {} 선택 시작: 목표 {}개, 현재 필요 {}개", difficulty, targetForDifficulty, remainingNeeded);
            
            // 독립 문항과 지문 그룹 비율 계산
            int independentTarget;
            int passageGroupTarget;
            
            if (request.isIncludePassage() && remainingNeeded > 5) {
                // 지문 포함이고 충분한 문항이 필요한 경우
                independentTarget = (int)(targetForDifficulty * 0.7);
                passageGroupTarget = targetForDifficulty - independentTarget;
            } else {
                // 지문 미포함이거나 적은 수의 문항만 필요한 경우
                independentTarget = targetForDifficulty;
                passageGroupTarget = 0;
            }

            // 독립 문항 선택
            if (independentTarget > 0 && remainingNeeded > 0) {
                List<ItemMetadata> independentItems =
                        itemMetadataRepository.findRandomItemsWithPassageGrouping(
                                request.getSubjectId(), request.getChapters(),
                                difficulty, Math.min(independentTarget, remainingNeeded), true);
                
                // 중복 제거
                independentItems = independentItems.stream()
                        .filter(item -> !selectedItemIds.contains(item.getItemId()))
                        .collect(Collectors.toList());
                
                if (!independentItems.isEmpty()) {
                    selectedItems.addAll(independentItems);
                    selectedItemIds.addAll(independentItems.stream()
                            .map(ItemMetadata::getItemId)
                            .collect(Collectors.toSet()));
                    remainingNeeded -= independentItems.size();
                    log.info("난이도 {} 독립 문항 {}개 선택, 남은 필요 수: {}", 
                            difficulty, independentItems.size(), remainingNeeded);
                }
            }

            // 지문 그룹 선택 (남은 개수가 충분할 때만)
            if (request.isIncludePassage() && passageGroupTarget > 0 && remainingNeeded >= 3) {
                // 지문 그룹은 최소 3개 이상의 문항이 필요할 때만 선택
                int maxPassageGroups = remainingNeeded / 3; // 평균적으로 지문당 3개 문항 가정
                int actualPassageTarget = Math.min(passageGroupTarget, maxPassageGroups);
                
                if (actualPassageTarget > 0) {
                    List<ItemMetadata> passageRepresentatives =
                            itemMetadataRepository.findRandomItemsWithPassageGrouping(
                                    request.getSubjectId(), request.getChapters(),
                                    difficulty, actualPassageTarget, false);

                    if (!passageRepresentatives.isEmpty()) {
                        List<Long> passageIds = passageRepresentatives.stream()
                                .map(ItemMetadata::getPassageId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .collect(Collectors.toList());

                        List<ItemMetadata> passageItems =
                                itemMetadataRepository.findItemsByPassageIds(request.getSubjectId(), passageIds);
                        
                        // 중복 제거
                        passageItems = passageItems.stream()
                                .filter(item -> !selectedItemIds.contains(item.getItemId()))
                                .collect(Collectors.toList());
                        
                        // 남은 개수를 초과하지 않도록 지문 그룹 조정
                        if (passageItems.size() > remainingNeeded) {
                            // 지문 그룹을 하나씩 제거하면서 적절한 수로 조정
                            Map<Long, List<ItemMetadata>> passageMap = passageItems.stream()
                                    .collect(Collectors.groupingBy(ItemMetadata::getPassageId));
                            
                            List<ItemMetadata> adjustedPassageItems = new ArrayList<>();
                            for (Map.Entry<Long, List<ItemMetadata>> passageEntry : passageMap.entrySet()) {
                                if (adjustedPassageItems.size() + passageEntry.getValue().size() <= remainingNeeded) {
                                    adjustedPassageItems.addAll(passageEntry.getValue());
                                    
                                    // 지문 그룹 정보 추가
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
                            }
                            passageItems = adjustedPassageItems;
                        } else {
                            // 모든 지문 그룹 정보 생성
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
                        }

                        if (!passageItems.isEmpty()) {
                            selectedItems.addAll(passageItems);
                            selectedItemIds.addAll(passageItems.stream()
                                    .map(ItemMetadata::getItemId)
                                    .collect(Collectors.toSet()));
                            remainingNeeded -= passageItems.size();
                            log.info("난이도 {} 지문 그룹 {}개(문항 {}개) 선택, 남은 필요 수: {}", 
                                    difficulty, passageIds.size(), passageItems.size(), remainingNeeded);
                        }
                    }
                }
            }
        }
        
        // Phase 2: 부족한 문항 채우기 - 모든 난이도에서 가능한 문항 수집
        if (remainingNeeded > 0) {
            log.info("Phase 2: {}개 문항 추가 필요", remainingNeeded);
            
            // 우선순위: 중간(2) -> 하(1) -> 상(3) 순서로 시도
            List<Long> fillPriority = Arrays.asList(2L, 1L, 3L);
            
            for (Long difficulty : fillPriority) {
                if (remainingNeeded <= 0) break;
                
                // 해당 난이도에서 가능한 최대한 많은 문항 조회
                int queryLimit = Math.min(remainingNeeded * 2, 100); // 여유있게 조회
                
                List<ItemMetadata> additionalItems =
                        itemMetadataRepository.findRandomItemsWithPassageGrouping(
                                request.getSubjectId(), request.getChapters(),
                                difficulty, queryLimit, true);
                
                // 이미 선택된 문항 제외하고 필요한 만큼만 선택
                List<ItemMetadata> filteredItems = additionalItems.stream()
                        .filter(item -> !selectedItemIds.contains(item.getItemId()))
                        .limit(remainingNeeded)
                        .collect(Collectors.toList());
                
                if (!filteredItems.isEmpty()) {
                    selectedItems.addAll(filteredItems);
                    selectedItemIds.addAll(filteredItems.stream()
                            .map(ItemMetadata::getItemId)
                            .collect(Collectors.toSet()));
                    remainingNeeded -= filteredItems.size();
                    log.info("난이도 {}에서 {}개 추가 선택, 남은 필요 수: {}", 
                            difficulty, filteredItems.size(), remainingNeeded);
                }
            }
        }
        
        // Phase 3: 그래도 부족하면 모든 조건 완화하여 선택
        if (remainingNeeded > 0) {
            log.info("Phase 3: 조건 완화하여 {}개 추가 선택 시도", remainingNeeded);
            
            // 모든 난이도에서 동시에 조회
            for (Long difficulty : Arrays.asList(1L, 2L, 3L)) {
                if (remainingNeeded <= 0) break;
                
                // 더 많은 문항 조회 (지문 포함)
                List<ItemMetadata> emergencyItems =
                        itemMetadataRepository.findRandomItemsWithPassageGrouping(
                                request.getSubjectId(), request.getChapters(),
                                difficulty, remainingNeeded * 3, false);
                
                List<ItemMetadata> filteredEmergency = emergencyItems.stream()
                        .filter(item -> !selectedItemIds.contains(item.getItemId()))
                        .limit(remainingNeeded)
                        .collect(Collectors.toList());
                
                if (!filteredEmergency.isEmpty()) {
                    selectedItems.addAll(filteredEmergency);
                    selectedItemIds.addAll(filteredEmergency.stream()
                            .map(ItemMetadata::getItemId)
                            .collect(Collectors.toSet()));
                    remainingNeeded -= filteredEmergency.size();
                    log.info("긴급 선택: 난이도 {}에서 {}개 추가", difficulty, filteredEmergency.size());
                }
            }
        }
        
        // Phase 4: 초과한 경우 정확히 맞추기
        if (selectedItems.size() > targetItemCount) {
            log.info("초과 문항 제거: {}개 -> {}개", selectedItems.size(), targetItemCount);
            
            // 지문 그룹을 우선적으로 제거 (큰 덩어리 제거)
            if (!passageGroups.isEmpty()) {
                List<SmartSelectionResponse.PassageGroupInfo> sortedGroups = new ArrayList<>(passageGroups);
                sortedGroups.sort((a, b) -> Integer.compare(b.getItemCount(), a.getItemCount()));
                
                for (SmartSelectionResponse.PassageGroupInfo group : sortedGroups) {
                    if (selectedItems.size() - group.getItemCount() >= targetItemCount) {
                        // 이 그룹을 제거해도 목표 개수 이상이면 제거
                        selectedItems.removeIf(item -> 
                                item.getPassageId() != null && 
                                item.getPassageId().equals(group.getPassageId()));
                        passageGroups.remove(group);
                        
                        if (selectedItems.size() <= targetItemCount) {
                            break;
                        }
                    }
                }
            }
            
            // 그래도 초과하면 뒤에서부터 개별 제거
            while (selectedItems.size() > targetItemCount) {
                selectedItems.remove(selectedItems.size() - 1);
            }
        }
        
        log.info("최종 선택 완료: 요청 {}개, 실제 {}개", targetItemCount, selectedItems.size());

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
        
        // 요청된 전체 개수
        int requestedTotal = targetCounts.values().stream().mapToInt(Integer::intValue).sum();

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
        
        // 최종 조정: 정확히 요청된 개수만큼 맞추기
        int currentTotal = adjusted.values().stream().mapToInt(Integer::intValue).sum();
        
        if (currentTotal != requestedTotal) {
            int difference = requestedTotal - currentTotal;
            log.info("최종 조정 필요: 현재 {}개, 목표 {}개, 차이 {}개", currentTotal, requestedTotal, difference);
            
            if (difference > 0) {
                // 더 필요한 경우: 가용한 난이도에 추가 할당
                for (Long difficulty : difficulties) {
                    if (difference <= 0) break;
                    
                    long available = availableCounts.get(difficulty).get("total");
                    int current = adjusted.get(difficulty);
                    int canAdd = (int)available - current;
                    
                    if (canAdd > 0) {
                        int toAdd = Math.min(canAdd, difference);
                        adjusted.put(difficulty, current + toAdd);
                        difference -= toAdd;
                        log.info("난이도 {}에 {}개 추가 할당", difficulty, toAdd);
                    }
                }
            } else if (difference < 0) {
                // 초과한 경우: 비율에 따라 감소
                int excess = -difference;
                for (Long difficulty : difficulties) {
                    if (excess <= 0) break;
                    
                    int current = adjusted.get(difficulty);
                    if (current > 0) {
                        // 현재 비율에 따라 감소
                        int toReduce = Math.min(current, excess);
                        adjusted.put(difficulty, current - toReduce);
                        excess -= toReduce;
                        log.info("난이도 {}에서 {}개 감소", difficulty, toReduce);
                    }
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

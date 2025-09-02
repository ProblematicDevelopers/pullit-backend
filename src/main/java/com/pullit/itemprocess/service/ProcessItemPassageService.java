package com.pullit.itemprocess.service;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pullit.itemprocess.dto.request.AddItemToPassageRequest;
import com.pullit.itemprocess.dto.request.CreatePassageGroupRequest;
import com.pullit.itemprocess.dto.response.PassageGroupResponse;
import com.pullit.itemprocess.entity.ProcessItemMetadata;
import com.pullit.itemprocess.repository.ProcessItemMetadataRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessItemPassageService {
    
    private final ProcessItemMetadataRepository processItemMetadataRepository;
    
    /**
     * 같은 subject의 지문 그룹별 문항 조회
     */
    public Map<Long, PassageGroupResponse> getPassageGroupsBySubject(Long subjectId) {
        log.info("[PassageService] 지문 그룹 조회 시작 - subjectId: {}", subjectId);
        
        List<ProcessItemMetadata> allItems = processItemMetadataRepository
            .findBySubject_SubjectId(subjectId);
        
        // passageId로 그룹핑
        Map<Long, List<ProcessItemMetadata>> passageGroups = allItems.stream()
            .filter(item -> item.getPassageId() != null)
            .collect(Collectors.groupingBy(ProcessItemMetadata::getPassageId));
        
        Map<Long, PassageGroupResponse> response = new HashMap<>();
        
        for (Map.Entry<Long, List<ProcessItemMetadata>> entry : passageGroups.entrySet()) {
            Long passageId = entry.getKey();
            List<ProcessItemMetadata> items = entry.getValue();
            
            // itemId로 정렬
            items.sort(Comparator.comparing(ProcessItemMetadata::getItemId));
            
            PassageGroupResponse groupResponse = buildPassageGroupResponse(passageId, subjectId, items);
            response.put(passageId, groupResponse);
        }
        
        log.info("[PassageService] 지문 그룹 조회 완료 - 총 {}개 그룹", response.size());
        return response;
    }
    
    /**
     * 독립 문항들 조회 (passageId가 null인 문항들)
     */
    public List<ProcessItemMetadata> getIndependentItemsBySubject(Long subjectId) {
        log.info("[PassageService] 독립 문항 조회 - subjectId: {}", subjectId);
        return processItemMetadataRepository.findIndependentItemsBySubjectId(subjectId);
    }
    
    /**
     * 지문 그룹의 대표 문항들 조회
     */
    public List<ProcessItemMetadata> getPassageRepresentativesBySubject(Long subjectId) {
        log.info("[PassageService] 지문 대표 문항 조회 - subjectId: {}", subjectId);
        return processItemMetadataRepository.findPassageRepresentativesBySubjectId(subjectId);
    }
    
    /**
     * 새로운 지문 그룹 생성
     */
    @Transactional
    public Long createPassageGroup(CreatePassageGroupRequest request) {
        log.info("[PassageService] 지문 그룹 생성 시작 - subjectId: {}, itemIds: {}", 
                request.getSubjectId(), request.getItemIds());
        
        Long newPassageId = generateNewPassageId();
        
        // 문항들을 지문 그룹에 추가
        for (Long itemId : request.getItemIds()) {
            ProcessItemMetadata item = processItemMetadataRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("ProcessItemMetadata not found: " + itemId));
            
            // subject 검증
            if (!item.getSubject().getSubjectId().equals(request.getSubjectId())) {
                throw new IllegalArgumentException("Item subject mismatch: " + itemId);
            }
            
            item.setPassageId(newPassageId);
            processItemMetadataRepository.save(item);
        }
        
        log.info("[PassageService] 지문 그룹 생성 완료 - passageId: {}", newPassageId);
        return newPassageId;
    }
    
    /**
     * 기존 지문 그룹에 문항 추가
     */
    @Transactional
    public void addItemsToPassage(AddItemToPassageRequest request) {
        log.info("[PassageService] 지문에 문항 추가 - passageId: {}, itemIds: {}", 
                request.getPassageId(), request.getItemIds());
        
        for (Long itemId : request.getItemIds()) {
            ProcessItemMetadata item = processItemMetadataRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("ProcessItemMetadata not found: " + itemId));
            
            item.setPassageId(request.getPassageId());
            processItemMetadataRepository.save(item);
        }
        
        log.info("[PassageService] 지문에 문항 추가 완료");
    }
    
    /**
     * 지문 그룹에서 문항 제거 (독립 문항으로 변경)
     */
    @Transactional
    public void removeItemsFromPassage(List<Long> itemIds) {
        log.info("[PassageService] 지문에서 문항 제거 - itemIds: {}", itemIds);
        
        for (Long itemId : itemIds) {
            ProcessItemMetadata item = processItemMetadataRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("ProcessItemMetadata not found: " + itemId));
            
            item.setPassageId(null);
            processItemMetadataRepository.save(item);
        }
        
        log.info("[PassageService] 지문에서 문항 제거 완료");
    }
    
    /**
     * 지문 그룹 삭제 (모든 문항을 독립 문항으로 변경)
     */
    @Transactional
    public void deletePassageGroup(Long passageId) {
        log.info("[PassageService] 지문 그룹 삭제 - passageId: {}", passageId);
        
        List<ProcessItemMetadata> items = processItemMetadataRepository
            .findAll().stream()
            .filter(item -> Objects.equals(item.getPassageId(), passageId))
            .collect(Collectors.toList());
        
        for (ProcessItemMetadata item : items) {
            item.setPassageId(null);
            processItemMetadataRepository.save(item);
        }
        
        log.info("[PassageService] 지문 그룹 삭제 완료 - {}개 문항이 독립 문항으로 변경", items.size());
    }
    
    /**
     * 지문 그룹 통계 조회
     */
    public Map<String, Object> getPassageStatistics(Long subjectId) {
        log.info("[PassageService] 지문 통계 조회 - subjectId: {}", subjectId);
        
        List<Object[]> passageCounts = processItemMetadataRepository.countItemsByPassageId(subjectId);
        List<ProcessItemMetadata> independentItems = getIndependentItemsBySubject(subjectId);
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("passageGroupCount", passageCounts.size());
        statistics.put("independentItemCount", independentItems.size());
        
        int totalPassageItems = passageCounts.stream()
            .mapToInt(arr -> ((Number) arr[1]).intValue())
            .sum();
        statistics.put("totalPassageItemCount", totalPassageItems);
        statistics.put("totalItemCount", totalPassageItems + independentItems.size());
        
        // 지문별 문항 수 분포
        Map<Long, Integer> passageItemCounts = passageCounts.stream()
            .collect(Collectors.toMap(
                arr -> (Long) arr[0],
                arr -> ((Number) arr[1]).intValue()
            ));
        statistics.put("passageItemCounts", passageItemCounts);
        
        log.info("[PassageService] 지문 통계 조회 완료 - 지문 그룹: {}, 독립 문항: {}, 총 문항: {}", 
                passageCounts.size(), independentItems.size(), 
                totalPassageItems + independentItems.size());
        
        return statistics;
    }
    
    /**
     * PassageGroupResponse 빌드
     */
    private PassageGroupResponse buildPassageGroupResponse(Long passageId, Long subjectId, 
                                                          List<ProcessItemMetadata> items) {
        if (items.isEmpty()) {
            return PassageGroupResponse.builder()
                .passageId(passageId)
                .subjectId(subjectId)
                .items(Collections.emptyList())
                .itemCount(0)
                .build();
        }
        
        // 첫 번째 문항에서 지문 내용 추출
        ProcessItemMetadata firstItem = items.get(0);
        String passageContent = null;
        String passageHtml = null;
        
        if (firstItem.getHtmlData() != null) {
            passageContent = firstItem.getHtmlData().getPassage();
            passageHtml = firstItem.getHtmlData().getPassageHtml();
        }
        
        // 대표 난이도 (가장 많은 난이도)
        String representativeDifficulty = items.stream()
            .collect(Collectors.groupingBy(
                item -> item.getDifficulty().getName(),
                Collectors.counting()
            ))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("미지정");
        
        // 최소/최대 itemId
        Long minItemId = items.stream().mapToLong(ProcessItemMetadata::getItemId).min().orElse(0L);
        Long maxItemId = items.stream().mapToLong(ProcessItemMetadata::getItemId).max().orElse(0L);
        
        // 생성/수정 시간
        String createdAt = firstItem.getCreatedDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String updatedAt = firstItem.getUpdatedDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        return PassageGroupResponse.builder()
            .passageId(passageId)
            .subjectId(subjectId)
            .items(items)
            .itemCount(items.size())
            .representativeDifficulty(representativeDifficulty)
            .passageContent(passageContent)
            .passageHtml(passageHtml)
            .minItemId(minItemId)
            .maxItemId(maxItemId)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }
    
    /**
     * 새로운 passageId 생성
     */
    private Long generateNewPassageId() {
        // 현재 시간 기반으로 고유한 ID 생성
        return System.currentTimeMillis();
    }
}

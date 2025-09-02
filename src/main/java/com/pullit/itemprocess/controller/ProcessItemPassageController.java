package com.pullit.itemprocess.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pullit.itemprocess.dto.request.AddItemToPassageRequest;
import com.pullit.itemprocess.dto.request.CreatePassageGroupRequest;
import com.pullit.itemprocess.dto.response.PassageGroupResponse;
import com.pullit.itemprocess.entity.ProcessItemMetadata;
import com.pullit.itemprocess.service.ProcessItemPassageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/process-items/passage-groups")
@RequiredArgsConstructor
public class ProcessItemPassageController {
    
    private final ProcessItemPassageService passageService;
    
    /**
     * 같은 subject의 지문 그룹 조회
     */
    @GetMapping("/subject/{subjectId}")
    public ResponseEntity<Map<Long, PassageGroupResponse>> getPassageGroups(
            @PathVariable Long subjectId) {
        
        log.info("[PassageController] 지문 그룹 조회 요청 - subjectId: {}", subjectId);
        
        Map<Long, PassageGroupResponse> passageGroups = 
            passageService.getPassageGroupsBySubject(subjectId);
        
        return ResponseEntity.ok(passageGroups);
    }
    
    /**
     * 독립 문항들 조회 (passageId가 null인 문항들)
     */
    @GetMapping("/subject/{subjectId}/independent")
    public ResponseEntity<List<ProcessItemMetadata>> getIndependentItems(
            @PathVariable Long subjectId) {
        
        log.info("[PassageController] 독립 문항 조회 요청 - subjectId: {}", subjectId);
        
        List<ProcessItemMetadata> independentItems = 
            passageService.getIndependentItemsBySubject(subjectId);
        
        return ResponseEntity.ok(independentItems);
    }
    
    /**
     * 지문 그룹의 대표 문항들 조회
     */
    @GetMapping("/subject/{subjectId}/representatives")
    public ResponseEntity<List<ProcessItemMetadata>> getPassageRepresentatives(
            @PathVariable Long subjectId) {
        
        log.info("[PassageController] 지문 대표 문항 조회 요청 - subjectId: {}", subjectId);
        
        List<ProcessItemMetadata> representatives = 
            passageService.getPassageRepresentativesBySubject(subjectId);
        
        return ResponseEntity.ok(representatives);
    }
    
    /**
     * 특정 지문 그룹의 상세 정보 조회
     */
    @GetMapping("/{passageId}")
    public ResponseEntity<PassageGroupResponse> getPassageGroupDetail(
            @PathVariable Long passageId) {
        
        log.info("[PassageController] 지문 그룹 상세 조회 요청 - passageId: {}", passageId);
        
        // 모든 subject에서 해당 passageId를 가진 문항들을 찾아서 그룹화
        // 실제로는 subjectId도 함께 받는 것이 좋지만, 현재는 passageId만으로 조회
        Map<Long, PassageGroupResponse> allGroups = passageService.getPassageGroupsBySubject(null);
        PassageGroupResponse group = allGroups.get(passageId);
        
        if (group == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(group);
    }
    
    /**
     * 새로운 지문 그룹 생성
     */
    @PostMapping
    public ResponseEntity<Long> createPassageGroup(
            @RequestBody CreatePassageGroupRequest request) {
        
        log.info("[PassageController] 지문 그룹 생성 요청 - subjectId: {}, itemIds: {}", 
                request.getSubjectId(), request.getItemIds());
        
        Long newPassageId = passageService.createPassageGroup(request);
        
        return ResponseEntity.ok(newPassageId);
    }
    
    /**
     * 기존 지문 그룹에 문항 추가
     */
    @PostMapping("/{passageId}/items")
    public ResponseEntity<Void> addItemsToPassage(
            @PathVariable Long passageId,
            @RequestBody List<Long> itemIds) {
        
        log.info("[PassageController] 지문에 문항 추가 요청 - passageId: {}, itemIds: {}", 
                passageId, itemIds);
        
        AddItemToPassageRequest request = AddItemToPassageRequest.builder()
            .passageId(passageId)
            .itemIds(itemIds)
            .build();
        
        passageService.addItemsToPassage(request);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * 지문 그룹에서 문항 제거 (독립 문항으로 변경)
     */
    @DeleteMapping("/items")
    public ResponseEntity<Void> removeItemsFromPassage(
            @RequestBody List<Long> itemIds) {
        
        log.info("[PassageController] 지문에서 문항 제거 요청 - itemIds: {}", itemIds);
        
        passageService.removeItemsFromPassage(itemIds);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * 지문 그룹 삭제 (모든 문항을 독립 문항으로 변경)
     */
    @DeleteMapping("/{passageId}")
    public ResponseEntity<Void> deletePassageGroup(
            @PathVariable Long passageId) {
        
        log.info("[PassageController] 지문 그룹 삭제 요청 - passageId: {}", passageId);
        
        passageService.deletePassageGroup(passageId);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * 지문 그룹 통계 조회
     */
    @GetMapping("/subject/{subjectId}/statistics")
    public ResponseEntity<Map<String, Object>> getPassageStatistics(
            @PathVariable Long subjectId) {
        
        log.info("[PassageController] 지문 통계 조회 요청 - subjectId: {}", subjectId);
        
        Map<String, Object> statistics = passageService.getPassageStatistics(subjectId);
        
        return ResponseEntity.ok(statistics);
    }
    
    /**
     * 지문 그룹의 문항 순서 변경
     */
    @PutMapping("/{passageId}/items/order")
    public ResponseEntity<Void> updateItemOrder(
            @PathVariable Long passageId,
            @RequestBody List<Long> orderedItemIds) {
        
        log.info("[PassageController] 지문 문항 순서 변경 요청 - passageId: {}, orderedItemIds: {}", 
                passageId, orderedItemIds);
        
        // TODO: 문항 순서 변경 로직 구현
        // 현재는 itemId 순서로 정렬되어 있지만, 별도의 order 필드가 필요할 수 있음
        
        return ResponseEntity.ok().build();
    }
}

package com.pullit.itemprocess.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pullit.itemprocess.dto.request.ProcessItemMetadataSearchRequest;
import com.pullit.itemprocess.dto.response.ProcessItemMetadataResponse;
import com.pullit.itemprocess.entity.ProcessItemHtmlData;
import com.pullit.itemprocess.entity.ProcessItemMetadata;
import com.pullit.itemprocess.repository.ProcessItemMetadataRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessItemMetadataService {
    
    private final ProcessItemMetadataRepository processItemMetadataRepository;
    
    /**
     * ProcessItemMetadata 검색 및 조회
     */
    public Page<ProcessItemMetadataResponse> searchProcessItemMetadata(ProcessItemMetadataSearchRequest request) {
        log.info("[ProcessItemMetadataService] 검색 요청 - subjectId: {}, keyword: {}", 
                request.getSubjectId(), request.getKeyword());
        
        List<ProcessItemMetadata> items = performSearch(request);
        
        // 페이징 처리
        Pageable pageable = createPageable(request);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), items.size());
        
        List<ProcessItemMetadata> pagedItems = items.subList(start, end);
        List<ProcessItemMetadataResponse> responses = pagedItems.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        return new PageImpl<>(responses, pageable, items.size());
    }
    
    /**
     * 단일 ProcessItemMetadata 조회
     */
    public ProcessItemMetadataResponse getProcessItemMetadata(Long itemId) {
        log.info("[ProcessItemMetadataService] 단일 문항 조회 - itemId: {}", itemId);
        
        ProcessItemMetadata item = processItemMetadataRepository.findById(itemId)
            .orElseThrow(() -> new IllegalArgumentException("ProcessItemMetadata not found: " + itemId));
        
        return convertToResponse(item);
    }
    
    /**
     * 같은 subject의 모든 ProcessItemMetadata 조회
     */
    public List<ProcessItemMetadataResponse> getAllProcessItemMetadataBySubject(Long subjectId) {
        log.info("[ProcessItemMetadataService] subject별 전체 문항 조회 - subjectId: {}", subjectId);
        
        List<ProcessItemMetadata> items = processItemMetadataRepository.findBySubject_SubjectId(subjectId);
        
        return items.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * 최근 생성된 ProcessItemMetadata 조회
     */
    public List<ProcessItemMetadataResponse> getRecentProcessItemMetadata(Long subjectId, int limit) {
        log.info("[ProcessItemMetadataService] 최근 문항 조회 - subjectId: {}, limit: {}", subjectId, limit);
        
        List<ProcessItemMetadata> items = processItemMetadataRepository.findRecentItemsBySubjectId(subjectId);
        
        return items.stream()
            .limit(limit)
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * ProcessItemMetadata 통계 조회
     */
    public Map<String, Object> getProcessItemMetadataStatistics(Long subjectId) {
        log.info("[ProcessItemMetadataService] 통계 조회 - subjectId: {}", subjectId);
        
        Map<String, Object> statistics = new HashMap<>();
        
        // 전체 문항 수
        Long totalCount = processItemMetadataRepository.countBySubjectId(subjectId);
        statistics.put("totalCount", totalCount);
        
        // 난이도별 문항 수
        List<Object[]> difficultyCounts = processItemMetadataRepository.countBySubjectIdAndDifficulty(subjectId);
        Map<Long, Long> difficultyMap = difficultyCounts.stream()
            .collect(Collectors.toMap(
                arr -> (Long) arr[0],
                arr -> (Long) arr[1]
            ));
        statistics.put("difficultyCounts", difficultyMap);
        
        // 문제 유형별 문항 수
        List<Object[]> questionFormCounts = processItemMetadataRepository.countBySubjectIdAndQuestionForm(subjectId);
        Map<Long, Long> questionFormMap = questionFormCounts.stream()
            .collect(Collectors.toMap(
                arr -> (Long) arr[0],
                arr -> (Long) arr[1]
            ));
        statistics.put("questionFormCounts", questionFormMap);
        
        // 지문 관련 통계
        List<ProcessItemMetadata> allItems = processItemMetadataRepository.findBySubject_SubjectId(subjectId);
        long passageItemCount = allItems.stream().filter(item -> item.getPassageId() != null).count();
        long independentItemCount = allItems.stream().filter(item -> item.getPassageId() == null).count();
        
        statistics.put("passageItemCount", passageItemCount);
        statistics.put("independentItemCount", independentItemCount);
        
        // HTML/이미지 데이터 통계
        long htmlDataCount = allItems.stream().filter(item -> Boolean.TRUE.equals(item.getHasHtmlData())).count();
        long imageDataCount = allItems.stream().filter(item -> Boolean.TRUE.equals(item.getHasImageData())).count();
        
        statistics.put("htmlDataCount", htmlDataCount);
        statistics.put("imageDataCount", imageDataCount);
        
        log.info("[ProcessItemMetadataService] 통계 조회 완료 - 총 {}개 문항", totalCount);
        return statistics;
    }
    
    /**
     * 검색 수행
     */
    private List<ProcessItemMetadata> performSearch(ProcessItemMetadataSearchRequest request) {
        List<ProcessItemMetadata> items = new ArrayList<>();
        
        // 기본 조건: subjectId가 있으면 해당 subject의 문항들만 조회
        if (request.getSubjectId() != null) {
            items = processItemMetadataRepository.findBySubject_SubjectId(request.getSubjectId());
        } else {
            items = processItemMetadataRepository.findAll();
        }
        
        // 필터링 적용
        items = applyFilters(items, request);
        
        // 정렬 적용
        items = applySorting(items, request);
        
        return items;
    }
    
    /**
     * 필터링 적용
     */
    private List<ProcessItemMetadata> applyFilters(List<ProcessItemMetadata> items, ProcessItemMetadataSearchRequest request) {
        return items.stream()
            .filter(item -> {
                // 난이도 필터
                if (request.getDifficultyCode() != null && 
                    !item.getDifficulty().getCode().equals(request.getDifficultyCode())) {
                    return false;
                }
                
                // 문제 유형 필터
                if (request.getQuestionFormCode() != null && 
                    !item.getQuestionForm().getCode().equals(request.getQuestionFormCode())) {
                    return false;
                }
                
                // 지문 ID 필터
                if (request.getPassageId() != null && 
                    !request.getPassageId().equals(item.getPassageId())) {
                    return false;
                }
                
                // 지문 여부 필터
                if (request.getHasPassage() != null) {
                    boolean hasPassage = item.getPassageId() != null;
                    if (request.getHasPassage() != hasPassage) {
                        return false;
                    }
                }
                
                // HTML 데이터 필터
                if (request.getHasHtmlData() != null && 
                    !request.getHasHtmlData().equals(item.getHasHtmlData())) {
                    return false;
                }
                
                // 이미지 데이터 필터
                if (request.getHasImageData() != null && 
                    !request.getHasImageData().equals(item.getHasImageData())) {
                    return false;
                }
                
                // 챕터 필터
                if (request.getChapterIds() != null && !request.getChapterIds().isEmpty()) {
                    boolean matchesChapter = false;
                    if (item.getChapterHierarchy() != null) {
                        if (item.getChapterHierarchy().getLargeChapter() != null && 
                            request.getChapterIds().contains(item.getChapterHierarchy().getLargeChapter().getCode())) {
                            matchesChapter = true;
                        }
                        if (item.getChapterHierarchy().getMediumChapter() != null && 
                            request.getChapterIds().contains(item.getChapterHierarchy().getMediumChapter().getCode())) {
                            matchesChapter = true;
                        }
                        if (item.getChapterHierarchy().getSmallChapter() != null && 
                            request.getChapterIds().contains(item.getChapterHierarchy().getSmallChapter().getCode())) {
                            matchesChapter = true;
                        }
                        if (item.getChapterHierarchy().getTopicChapter() != null && 
                            request.getChapterIds().contains(item.getChapterHierarchy().getTopicChapter().getCode())) {
                            matchesChapter = true;
                        }
                    }
                    if (!matchesChapter) {
                        return false;
                    }
                }
                
                // 키워드 필터 (지문, 문제, 답안, 해설에서 검색)
                if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                    String keyword = request.getKeyword().toLowerCase();
                    boolean matchesKeyword = false;
                    
                    if (item.getHtmlData() != null) {
                        ProcessItemHtmlData htmlData = item.getHtmlData();
                        if ((htmlData.getPassage() != null && htmlData.getPassage().toLowerCase().contains(keyword)) ||
                            (htmlData.getQuestion() != null && htmlData.getQuestion().toLowerCase().contains(keyword)) ||
                            (htmlData.getAnswer() != null && htmlData.getAnswer().toLowerCase().contains(keyword)) ||
                            (htmlData.getExplainText() != null && htmlData.getExplainText().toLowerCase().contains(keyword))) {
                            matchesKeyword = true;
                        }
                    }
                    
                    if (!matchesKeyword) {
                        return false;
                    }
                }
                
                return true;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 정렬 적용
     */
    private List<ProcessItemMetadata> applySorting(List<ProcessItemMetadata> items, ProcessItemMetadataSearchRequest request) {
        if (request.getSortBy() == null || request.getSortBy().trim().isEmpty()) {
            // 기본 정렬: 생성일 내림차순
            return items.stream()
                .sorted((a, b) -> b.getCreatedDate().compareTo(a.getCreatedDate()))
                .collect(Collectors.toList());
        }
        
        String sortBy = request.getSortBy();
        boolean ascending = !"DESC".equalsIgnoreCase(request.getSortDirection());
        
        return items.stream()
            .sorted((a, b) -> {
                int result = 0;
                switch (sortBy.toLowerCase()) {
                    case "itemid":
                        result = a.getItemId().compareTo(b.getItemId());
                        break;
                    case "createddate":
                        result = a.getCreatedDate().compareTo(b.getCreatedDate());
                        break;
                    case "updateddate":
                        result = a.getUpdatedDate().compareTo(b.getUpdatedDate());
                        break;
                    case "difficulty":
                        result = a.getDifficulty().getCode().compareTo(b.getDifficulty().getCode());
                        break;
                    case "questionform":
                        result = a.getQuestionForm().getCode().compareTo(b.getQuestionForm().getCode());
                        break;
                    case "passageid":
                        Long passageIdA = a.getPassageId() != null ? a.getPassageId() : 0L;
                        Long passageIdB = b.getPassageId() != null ? b.getPassageId() : 0L;
                        result = passageIdA.compareTo(passageIdB);
                        break;
                    default:
                        result = a.getCreatedDate().compareTo(b.getCreatedDate());
                }
                return ascending ? result : -result;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Pageable 생성
     */
    private Pageable createPageable(ProcessItemMetadataSearchRequest request) {
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;
        
        if (request.getSortBy() != null && !request.getSortBy().trim().isEmpty()) {
            Sort.Direction direction = "DESC".equalsIgnoreCase(request.getSortDirection()) ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
            Sort sort = Sort.by(direction, request.getSortBy());
            return PageRequest.of(page, size, sort);
        }
        
        return PageRequest.of(page, size);
    }
    
    /**
     * ProcessItemMetadata를 Response로 변환
     */
    private ProcessItemMetadataResponse convertToResponse(ProcessItemMetadata item) {
        ProcessItemMetadataResponse.ProcessItemMetadataResponseBuilder builder = ProcessItemMetadataResponse.builder()
            .itemId(item.getItemId())
            .sourceItemId(item.getSourceItemId())
            .subject(item.getSubject())
            .questionForm(item.getQuestionForm())
            .difficulty(item.getDifficulty())
            .chapterHierarchy(item.getChapterHierarchy())
            .passageId(item.getPassageId())
            .hasHtmlData(item.getHasHtmlData())
            .hasImageData(item.getHasImageData())
            .createdDate(item.getCreatedDate())
            .updatedDate(item.getUpdatedDate())
            .isPassageGroup(item.getPassageId() != null);
        
        // HTML 데이터가 있으면 포함
        if (item.getHtmlData() != null) {
            ProcessItemHtmlData htmlData = item.getHtmlData();
            builder.htmlData(htmlData)
                .passageContent(htmlData.getPassage())
                .passageHtml(htmlData.getPassageHtml())
                .questionContent(htmlData.getQuestion())
                .questionHtml(htmlData.getQuestionHtml())
                .answerContent(htmlData.getAnswer())
                .answerHtml(htmlData.getAnswerHtml())
                .explainContent(htmlData.getExplainText())
                .explainHtml(htmlData.getExplainHtml());
            
            // 선택지 정보
            List<String> choices = new ArrayList<>();
            if (htmlData.getChoice1Html() != null) choices.add(htmlData.getChoice1Html());
            if (htmlData.getChoice2Html() != null) choices.add(htmlData.getChoice2Html());
            if (htmlData.getChoice3Html() != null) choices.add(htmlData.getChoice3Html());
            if (htmlData.getChoice4Html() != null) choices.add(htmlData.getChoice4Html());
            if (htmlData.getChoice5Html() != null) choices.add(htmlData.getChoice5Html());
            builder.choices(choices);
        }
        
        // 이미지 데이터가 있으면 포함
        if (item.getImageData() != null) {
            builder.imageData(item.getImageData());
        }
        
        // 지문 그룹 크기 계산
        if (item.getPassageId() != null) {
            List<ProcessItemMetadata> passageGroup = processItemMetadataRepository
                .findBySubject_SubjectIdAndPassageId(item.getSubject().getSubjectId(), item.getPassageId());
            builder.passageGroupSize(passageGroup.size());
        } else {
            builder.passageGroupSize(1);
        }
        
        return builder.build();
    }
}

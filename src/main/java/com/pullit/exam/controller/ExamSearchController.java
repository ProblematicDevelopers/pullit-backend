package com.pullit.exam.controller;

import com.pullit.exam.dto.request.ExamSearchRequest;
import com.pullit.exam.dto.response.ExamCountBySubjectResponse;
import com.pullit.exam.dto.response.UnifiedExamResponse;
import com.pullit.exam.service.ExamSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/exams")
@RequiredArgsConstructor
@Tag(name="Exam Search", description = "시험 검색 API")
public class ExamSearchController {
    private final ExamSearchService examSearchService;
    /**
     * 통합 검색
     * - Exam과 UserExam을 모두 검색
     * - 키워드, 과목, 공개범위, 날짜 등 다양한 조건으로 검색
     */
    @GetMapping("/search")
    @Operation(summary = "시험 통합 검색", description = "TestWizard 시험과 사용자 생성 시험을 통합 검색합니다")
    public ResponseEntity<Page<UnifiedExamResponse>> searchExams(
            @ModelAttribute ExamSearchRequest request,
            @PageableDefault(size = 20, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("통합 검색 요청: keyword={}, subjectId={}, visibility={}",
                request.getKeyword(), request.getSubjectId(), request.getVisibility());

        Page<UnifiedExamResponse> results = examSearchService.searchExams(request, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * 빠른 검색 (자동완성용)
     * - 시험명 기준으로 빠르게 검색
     * - 최대 10개까지 반환
     */
    @GetMapping("/quick-search")
    @Operation(summary = "빠른 검색", description = "자동완성을 위한 빠른 검색 (최대 10개)")
    public ResponseEntity<List<UnifiedExamResponse>> quickSearch(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("빠른 검색 요청: keyword={}, limit={}", keyword, limit);

        List<UnifiedExamResponse> results = examSearchService.quickSearch(keyword, limit);
        return ResponseEntity.ok(results);
    }

    /**
     * 최근 생성된 시험 조회
     * - 생성일 기준 최신순 정렬
     */
    @GetMapping("/recent")
    @Operation(summary = "최근 시험 조회", description = "최근 생성된 시험을 조회합니다")
    public ResponseEntity<List<UnifiedExamResponse>> getRecentExams(
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("최근 시험 조회: limit={}", limit);

        List<UnifiedExamResponse> results = examSearchService.getRecentExams(limit);
        return ResponseEntity.ok(results);
    }

    /**
     * 사용자가 접근 가능한 시험 조회
     * - 본인이 생성한 시험
     * - PUBLIC 공개 시험
     * - SCHOOL 시험 (추후 Teacher 구현 시)
     */
    @GetMapping("/accessible")
    @Operation(summary = "접근 가능한 시험 조회", description = "현재 사용자가 접근 가능한 모든 시험을 조회합니다")
    public ResponseEntity<Page<UnifiedExamResponse>> getAccessibleExams(
            @RequestParam Long userId,  // 추후 @AuthenticationPrincipal로 변경
            @PageableDefault(size = 20, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("접근 가능 시험 조회: userId={}", userId);

        Page<UnifiedExamResponse> results = examSearchService.getAccessibleExams(userId, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * 시험 타입별 검색
     * - TESTWIZARD: Exam 테이블만
     * - USER_CREATED: UserExam 테이블만
     * - ALL: 모두 검색
     */
    @GetMapping("/search/by-type")
    @Operation(summary = "시험 타입별 검색", description = "특정 타입의 시험만 검색합니다")
    public ResponseEntity<Page<UnifiedExamResponse>> searchByType(
            @ModelAttribute ExamSearchRequest request,
            @RequestParam(defaultValue = "ALL") String examType,
            @PageableDefault(size = 20, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("타입별 검색: type={}, keyword={}", examType, request.getKeyword());

        Page<UnifiedExamResponse> results = examSearchService.searchByType(request, examType, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * 인기 시험 조회
     * - 사용 횟수 기준 정렬
     */
    @GetMapping("/popular")
    @Operation(summary = "인기 시험 조회", description = "가장 많이 사용된 시험을 조회합니다")
    public ResponseEntity<List<UnifiedExamResponse>> getPopularExams(
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("인기 시험 조회: limit={}", limit);

        List<UnifiedExamResponse> results = examSearchService.getPopularExams(limit);
        return ResponseEntity.ok(results);
    }

    /**
     * 추천 시험 조회
     * - 사용자 맞춤 추천 (추후 구현)
     */
    @GetMapping("/recommended")
    @Operation(summary = "추천 시험 조회", description = "사용자에게 추천하는 시험을 조회합니다")
    public ResponseEntity<List<UnifiedExamResponse>> getRecommendedExams(
            @RequestParam Long userId,  // 추후 @AuthenticationPrincipal로 변경
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("추천 시험 조회: userId={}, limit={}", userId, limit);

        List<UnifiedExamResponse> results = examSearchService.getRecommendedExams(userId, limit);
        return ResponseEntity.ok(results);
    }

    /**
     * 과목별 시험 통계 조회
     * - 각 과목별 시험 개수 집계
     */
    @GetMapping("/statistics/subject")
    @Operation(summary = "과목별 시험 통계", description = "과목별 시험 개수를 조회합니다")
    public ResponseEntity<List<ExamCountBySubjectResponse>> getExamCountBySubject(
            @RequestParam(required = false) Long subjectId
    ) {
        log.info("과목별 통계 조회: subjectId={}", subjectId);

        List<ExamCountBySubjectResponse> results = examSearchService.getExamCountBySubject(subjectId);
        return ResponseEntity.ok(results);
    }

    /**
     * 대단원별 검색 (Exam만 해당)
     * - TestWizard 시험의 대단원 기준 검색
     */
    @GetMapping("/search/by-chapter")
    @Operation(summary = "대단원별 검색", description = "특정 대단원의 시험을 검색합니다 (TestWizard만)")
    public ResponseEntity<Page<UnifiedExamResponse>> searchByChapter(
            @RequestParam Long largeChapterCode,
            @PageableDefault(size = 20, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("대단원별 검색: chapterCode={}", largeChapterCode);

        Page<UnifiedExamResponse> results = examSearchService.searchByChapter(largeChapterCode, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * 고급 검색
     * - 복합 조건으로 상세 검색
     * - 삭제된 항목 포함 옵션
     */
    @PostMapping("/search/advanced")
    @Operation(summary = "고급 검색", description = "복합 조건으로 상세 검색을 수행합니다")
    public ResponseEntity<Page<UnifiedExamResponse>> advancedSearch(
            @RequestBody ExamSearchRequest request,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @PageableDefault(size = 20, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("고급 검색: conditions={}, includeDeleted={}", request.hasSearchCondition(), includeDeleted);

        Page<UnifiedExamResponse> results = examSearchService.advancedSearch(request, includeDeleted, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * 시험지 개수 조회
     * - 조건에 맞는 시험지 개수를 조회합니다
     */
    @GetMapping("/count")
    @Operation(summary = "시험지 개수 조회", description = "조건에 맞는 시험지 개수를 조회합니다")
    public ResponseEntity<Map<String, Object>> getExamCount(
            @ModelAttribute ExamSearchRequest request
    ) {
        log.info("시험지 개수 조회 요청: areaCode={}, gradeCode={}", 
                request.getAreaCode(), request.getGradeCode());
        
        Map<String, Object> countData = new HashMap<>();
        
        try {
            // 전체 검색을 수행하여 실제 개수 가져오기
            Page<UnifiedExamResponse> result = examSearchService.searchExams(
                request, 
                Pageable.ofSize(1) // 개수만 필요하므로 1개만 조회
            );
            
            long totalCount = result.getTotalElements();
            
            // 응답 데이터 구성
            countData.put("totalCount", totalCount);
            
            // TestWizard와 UserCreated 개수 (향후 구분 가능하면 추가)
            countData.put("testWizardCount", totalCount); // 현재는 전체 개수로 설정
            countData.put("userCreatedCount", 0);
            
            // 공개 범위별 개수 (추가 쿼리 필요시 구현)
            countData.put("publicCount", totalCount);
            countData.put("schoolCount", 0);
            countData.put("privateCount", 0);
            
            log.info("시험지 개수 조회 성공: totalCount={}", totalCount);
        } catch (Exception e) {
            log.error("시험지 개수 조회 중 오류 발생", e);
            // 오류 시 기본값 반환
            countData.put("totalCount", 0);
            countData.put("testWizardCount", 0);
            countData.put("userCreatedCount", 0);
            countData.put("publicCount", 0);
            countData.put("schoolCount", 0);
            countData.put("privateCount", 0);
        }
        
        return ResponseEntity.ok(countData);
    }
    
    /**
     * 필터 옵션 조회
     * - 학년, 과목, 학기 등의 필터 옵션을 조회합니다
     */
    @GetMapping("/filters")
    @Operation(summary = "필터 옵션 조회", description = "검색 필터에 사용할 옵션들을 조회합니다")
    public ResponseEntity<Map<String, Object>> getFilterOptions() {
        log.info("필터 옵션 조회 요청");
        
        Map<String, Object> filterOptions = examSearchService.getFilterOptions();
        
        return ResponseEntity.ok(filterOptions);
    }

}

package com.pullit.exam.service;

import com.pullit.exam.dto.request.ExamSearchRequest;
import com.pullit.exam.dto.response.ExamCountBySubjectResponse;
import com.pullit.exam.dto.response.UnifiedExamResponse;
import com.pullit.exam.repository.ExamSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class ExamSearchServiceImpl implements ExamSearchService {
    private final ExamSearchRepository examSearchRepository;
    private final EntityManager entityManager;

    /**
     * 통합 검색 메인 메서드
     * - Exam과 UserExam 모두 검색
     * - 다양한 필터 조건 적용
     */
    @Override
    public Page<UnifiedExamResponse> searchExams(ExamSearchRequest request, Pageable pageable) {
        log.debug("통합 시험 검색 시작: {}", request);

        // 검색 조건 검증
        validateSearchRequest(request);

        // 통합 검색 실행
        Page<UnifiedExamResponse> result = examSearchRepository.searchUnified(request, pageable);

        log.debug("통합 시험 검색 완료: {} 건", result.getTotalElements());
        return result;
    }

    /**
     * 키워드 빠른 검색 (자동완성용)
     * - 캐싱으로 응답 속도 향상
     */
    @Override
    @Cacheable(value = "examQuickSearch", key = "#keyword + ':' + #limit")
    public List<UnifiedExamResponse> quickSearch(String keyword, int limit) {
        log.debug("빠른 검색: keyword={}, limit={}", keyword, limit);

        // 키워드 최소 길이 체크
        if (keyword == null || keyword.trim().length() < 2) {
            return List.of();
        }

        // limit 범위 체크 (최대 20개)
        int searchLimit = Math.min(limit, 20);

        return examSearchRepository.quickSearch(keyword.trim(), searchLimit);
    }

    /**
     * 사용자 접근 가능한 모든 시험 조회
     * - 본인 생성 시험
     * - PUBLIC 시험
     * - SCHOOL 시험 (같은 학교) - Teacher 엔티티 구현 후
     */
    @Override
    public Page<UnifiedExamResponse> getAccessibleExams(Long userId, Pageable pageable) {
        log.debug("사용자 {} 접근 가능 시험 조회", userId);

        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다");
        }

        return examSearchRepository.findAccessibleExams(userId, pageable);
    }

    /**
     * 최근 생성된 시험 조회
     * - 캐싱으로 자주 조회되는 데이터 최적화
     */
    @Override
    @Cacheable(value = "recentExams", key = "#limit")
    public List<UnifiedExamResponse> getRecentExams(int limit) {
        log.debug("최근 시험 조회: limit={}", limit);

        // limit 범위 체크 (최대 50개)
        int searchLimit = Math.min(limit, 50);

        return examSearchRepository.findRecentExams(searchLimit);
    }

    /**
     * 시험 타입별 검색
     * - TESTWIZARD: Exam 테이블만
     * - USER_CREATED: UserExam 테이블만
     * - ALL: 모두 (기본값)
     */
    @Override
    public Page<UnifiedExamResponse> searchByType(
            ExamSearchRequest request,
            String examType,
            Pageable pageable) {

        log.debug("타입별 검색: type={}", examType);

        // examType 검증
        String validExamType = validateExamType(examType);

        return examSearchRepository.searchByType(request, validExamType, pageable);
    }

    /**
     * 대단원별 시험 검색 (Exam만)
     * - UserExam은 chapter 정보 없음
     */
    @Override
    public Page<UnifiedExamResponse> searchByChapter(Long largeChapterCode, Pageable pageable) {
        log.debug("대단원별 검색: chapterCode={}", largeChapterCode);

        if (largeChapterCode == null) {
            throw new IllegalArgumentException("대단원 코드는 필수입니다");
        }

        return examSearchRepository.findByLargeChapter(largeChapterCode, pageable);
    }

    /**
     * 과목별 시험 개수 통계
     */
    @Override
    @Cacheable(value = "examCountBySubject", key = "#subjectId")
    public List<ExamCountBySubjectResponse> getExamCountBySubject(Long subjectId) {
        log.debug("과목별 시험 개수 조회: subjectId={}", subjectId);

        return examSearchRepository.countExamsBySubject(subjectId);
    }

    /**
     * 고급 검색
     * - 복잡한 조건 조합
     * - 삭제된 항목 포함 옵션
     */
    @Override
    public Page<UnifiedExamResponse> advancedSearch(
            ExamSearchRequest request,
            boolean includeDeleted,
            Pageable pageable) {

        log.debug("고급 검색: includeDeleted={}", includeDeleted);

        // 검색 조건 검증
        validateSearchRequest(request);

        return examSearchRepository.advancedSearch(request, includeDeleted, pageable);
    }

    /**
     * 인기 시험 조회
     * - 추후 조회수, 사용 횟수 등 통계 테이블 구현 후 개발
     */
    @Override
    public List<UnifiedExamResponse> getPopularExams(int limit) {
        log.debug("인기 시험 조회: limit={}", limit);

        // TODO: 통계 테이블 구현 후 실제 로직 구현
        // 임시로 최근 시험 반환
        return getRecentExams(limit);
    }

    /**
     * 추천 시험 조회
     * - 추후 사용자 프로필 기반 추천 알고리즘 구현
     */
    @Override
    public List<UnifiedExamResponse> getRecommendedExams(Long userId, int limit) {
        log.debug("추천 시험 조회: userId={}, limit={}", userId, limit);

        // TODO: 추천 알고리즘 구현
        // 임시로 최근 PUBLIC 시험 반환
        ExamSearchRequest request = ExamSearchRequest.builder()
                .visibility(com.pullit.exam.enums.ExamVisibility.PUBLIC)
                .build();

        Page<UnifiedExamResponse> result = examSearchRepository.searchUnified(
                request,
                Pageable.ofSize(limit)
        );

        return result.getContent();
    }

    // ===== Private Helper Methods =====

    /**
     * 검색 요청 검증
     */
    private void validateSearchRequest(ExamSearchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("검색 조건은 필수입니다");
        }

        // 날짜 범위 검증
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new IllegalArgumentException("시작일은 종료일보다 이전이어야 합니다");
            }
        }
    }

    /**
     * 시험 타입 검증
     */
    private String validateExamType(String examType) {
        if (examType == null || examType.trim().isEmpty()) {
            return "ALL";
        }

        String upperType = examType.toUpperCase();
        if (!List.of("TESTWIZARD", "USER_CREATED", "ALL").contains(upperType)) {
            log.warn("잘못된 examType: {}, ALL로 대체", examType);
            return "ALL";
        }

        return upperType;
    }

    /**
     * 필터 옵션 조회
     * - 실제 데이터베이스에서 학년, 과목, 학기, 교과서 옵션을 조회
     */
    @Override
    @Cacheable("filterOptions")
    @SuppressWarnings("unchecked")
    public Map<String, Object> getFilterOptions() {
        log.debug("필터 옵션 조회 시작");
        
        Map<String, Object> filterOptions = new HashMap<>();
        
        try {
            // 학년 옵션 조회
            String gradeQuery = """
                SELECT DISTINCT s.grade_code, s.grade_name, COUNT(e.exam_id) as exam_count
                FROM subjects s 
                LEFT JOIN exams e ON s.subject_id = e.subject_id 
                WHERE s.grade_code IS NOT NULL 
                GROUP BY s.grade_code, s.grade_name 
                ORDER BY s.grade_code
                """;
            
            Query gradeNativeQuery = entityManager.createNativeQuery(gradeQuery);
            List<Object[]> gradeResults = gradeNativeQuery.getResultList();
            
            List<Map<String, Object>> grades = gradeResults.stream()
                .map(row -> Map.of(
                    "code", row[0],
                    "name", row[1],
                    "count", ((Number) row[2]).intValue()
                ))
                .toList();

            // 과목 옵션 조회 (area_code 기준)
            String subjectQuery = """
                SELECT DISTINCT s.area_code, s.area_name, COUNT(e.exam_id) as exam_count
                FROM subjects s 
                LEFT JOIN exams e ON s.subject_id = e.subject_id 
                WHERE s.area_code IS NOT NULL 
                GROUP BY s.area_code, s.area_name 
                ORDER BY s.area_code
                """;
                
            Query subjectNativeQuery = entityManager.createNativeQuery(subjectQuery);
            List<Object[]> subjectResults = subjectNativeQuery.getResultList();
            
            List<Map<String, Object>> subjects = subjectResults.stream()
                .map(row -> Map.of(
                    "code", row[0],
                    "name", row[1],
                    "count", ((Number) row[2]).intValue()
                ))
                .toList();

            // 학기 옵션 조회
            String termQuery = """
                SELECT DISTINCT s.term_code, s.term_name, COUNT(e.exam_id) as exam_count
                FROM subjects s 
                LEFT JOIN exams e ON s.subject_id = e.subject_id 
                WHERE s.term_code IS NOT NULL AND s.term_code != '99'
                GROUP BY s.term_code, s.term_name 
                ORDER BY s.term_code
                """;
                
            Query termNativeQuery = entityManager.createNativeQuery(termQuery);
            List<Object[]> termResults = termNativeQuery.getResultList();
            
            List<Map<String, Object>> terms = termResults.stream()
                .map(row -> Map.of(
                    "code", row[0],
                    "name", row[1],
                    "count", ((Number) row[2]).intValue()
                ))
                .toList();

            // 교과서 옵션 조회 (최신 데이터만)
            String textbookQuery = """
                SELECT s.subject_id, s.subject_name, s.area_code, s.grade_code, COUNT(e.exam_id) as exam_count
                FROM subjects s 
                LEFT JOIN exams e ON s.subject_id = e.subject_id 
                WHERE s.subject_id IS NOT NULL 
                GROUP BY s.subject_id, s.subject_name, s.area_code, s.grade_code 
                ORDER BY s.area_code, s.grade_code, s.subject_name
                """;
                
            Query textbookNativeQuery = entityManager.createNativeQuery(textbookQuery);
            List<Object[]> textbookResults = textbookNativeQuery.getResultList();
            
            List<Map<String, Object>> textbooks = textbookResults.stream()
                .map(row -> Map.of(
                    "id", ((Number) row[0]).longValue(),
                    "name", row[1],
                    "areaCode", row[2],
                    "gradeCode", row[3],
                    "count", ((Number) row[4]).intValue()
                ))
                .toList();

            filterOptions.put("grades", grades);
            filterOptions.put("subjects", subjects);
            filterOptions.put("terms", terms);
            filterOptions.put("textbooks", textbooks);
            
            log.debug("필터 옵션 조회 완료: 학년={}, 과목={}, 학기={}, 교과서={}", 
                grades.size(), subjects.size(), terms.size(), textbooks.size());
            
        } catch (Exception e) {
            log.error("필터 옵션 조회 중 오류 발생", e);
            // 오류 시 기본값 반환
            filterOptions.put("grades", List.of());
            filterOptions.put("subjects", List.of());
            filterOptions.put("terms", List.of());
            filterOptions.put("textbooks", List.of());
        }
        
        return filterOptions;
    }

}

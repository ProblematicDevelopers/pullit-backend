package com.pullit.exam.service;


import com.pullit.exam.dto.request.ExamSearchRequest;
import com.pullit.exam.dto.response.ExamCountBySubjectResponse;
import com.pullit.exam.dto.response.ExamWithItemsResponse;
import com.pullit.exam.dto.response.UnifiedExamResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ExamSearchService {
    /**
     * 통합 검색
     * - Exam과 UserExam 모두 검색
     * - 검색 조건에 따라 필터링
     *
     * @param request 검색 조건
     * @param pageable 페이징
     * @return 통합 검색 결과
     */
    Page<UnifiedExamResponse> searchExams(ExamSearchRequest request, Pageable pageable);

    /**
     * 키워드 빠른 검색 (자동완성용)
     *
     * @param keyword 검색 키워드
     * @param limit 최대 결과 수
     * @return 검색 결과
     */
    List<UnifiedExamResponse> quickSearch(String keyword, int limit);

    /**
     * 사용자 접근 가능한 모든 시험 조회
     * - 본인 생성 시험
     * - PUBLIC 시험
     * - SCHOOL 시험 (같은 학교)
     *
     * @param userId 사용자 ID
     * @param pageable 페이징
     * @return 접근 가능한 시험 목록
     */
    Page<UnifiedExamResponse> getAccessibleExams(Long userId, Pageable pageable);

    /**
     * 최근 생성된 시험 조회
     *
     * @param limit 조회할 개수
     * @return 최근 시험 목록
     */
    List<UnifiedExamResponse> getRecentExams(int limit);

    /**
     * 시험 타입별 검색
     *
     * @param request 검색 조건
     * @param examType 시험 타입 (TESTWIZARD, USER_CREATED, ALL)
     * @param pageable 페이징
     * @return 필터링된 검색 결과
     */
    Page<UnifiedExamResponse> searchByType(ExamSearchRequest request, String examType, Pageable pageable);

    /**
     * 대단원별 시험 검색 (Exam만)
     *
     * @param largeChapterCode 대단원 코드
     * @param pageable 페이징
     * @return 해당 대단원 시험 목록
     */
    Page<UnifiedExamResponse> searchByChapter(Long largeChapterCode, Pageable pageable);

    /**
     * 과목별 시험 개수 통계
     *
     * @param subjectId 과목 ID (null이면 전체)
     * @return 과목별 시험 개수
     */
    List<ExamCountBySubjectResponse> getExamCountBySubject(Long subjectId);

    /**
     * 고급 검색
     * - 복잡한 조건 조합
     * - 삭제된 항목 포함 옵션
     *
     * @param request 검색 조건
     * @param includeDeleted 삭제된 UserExam 포함 여부
     * @param pageable 페이징
     * @return 검색 결과
     */
    Page<UnifiedExamResponse> advancedSearch(ExamSearchRequest request, boolean includeDeleted, Pageable pageable);

    /**
     * 인기 시험 조회
     * - 조회수, 사용 횟수 등을 기준으로
     *
     * @param limit 조회할 개수
     * @return 인기 시험 목록
     */
    List<UnifiedExamResponse> getPopularExams(int limit);

    /**
     * 추천 시험 조회
     * - 사용자 프로필 기반 추천
     *
     * @param userId 사용자 ID
     * @param limit 조회할 개수
     * @return 추천 시험 목록
     */
    List<UnifiedExamResponse> getRecommendedExams(Long userId, int limit);

    /**
     * 내가 생성한 시험 목록 조회
     * - 현재 사용자가 생성한 모든 시험 조회 (Exam, UserExam 모두 포함)
     *
     * @param userId 사용자 ID
     * @param pageable 페이징
     * @return 내가 생성한 시험 목록
     */
    Page<UnifiedExamResponse> getMyExams(Long userId, Pageable pageable);

    /**
     * 필터 옵션 조회
     * - 학년, 과목, 학기, 교과서 등의 필터 옵션을 실제 데이터에서 조회
     *
     * @return 필터 옵션 데이터
     */
    Map<String, Object> getFilterOptions();

    List<Long> getExamItemIds(Long examId);

    ExamWithItemsResponse getExamWithItems(Long examId);
}

package com.pullit.exam.repository;

import com.pullit.exam.dto.request.ExamSearchRequest;
import com.pullit.exam.dto.response.ExamCountBySubjectResponse;
import com.pullit.exam.dto.response.UnifiedExamResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ExamSearchRepository {

    /**
     * 통합 검색 - Exam과 UserExam을 모두 검색
     *
     * @param request 검색 조건 (keyword, subjectId, gradeCode, visibility 등)
     * @param pageable 페이징 정보
     * @return 통합된 검색 결과 (UnifiedExamResponse 형태)
     */
    Page<UnifiedExamResponse> searchUnified(ExamSearchRequest request, Pageable pageable);

    /**
     * 키워드로 빠른 검색 (자동완성용)
     * - 시험명 기준으로만 빠르게 검색
     * - Exam과 UserExam 모두에서 검색
     *
     * @param keyword 검색 키워드
     * @param limit 결과 제한 수 (최대 반환 개수)
     * @return 검색 결과 목록
     */
    List<UnifiedExamResponse> quickSearch(String keyword, int limit);

    /**
     * 사용자의 모든 접근 가능한 시험 조회
     * - 본인이 생성한 시험 (Exam, UserExam 모두)
     * - PUBLIC 공개 시험
     * - SCHOOL 시험 (Teacher 엔티티 구현 후 추가)
     *
     * @param userId 사용자 ID (Long 타입)
     * @param pageable 페이징
     * @return 접근 가능한 시험 목록
     */
    Page<UnifiedExamResponse> findAccessibleExams(Long userId, Pageable pageable);

    /**
     * 과목별 시험 통계 조회
     * - 각 과목별 시험 개수 집계
     * - Exam과 UserExam 합산
     *
     * @param subjectId 과목 ID (null이면 전체 과목)
     * @return 과목별 시험 개수 리스트
     */
    List<ExamCountBySubjectResponse> countExamsBySubject(Long subjectId);

    /**
     * 최근 생성된 시험 조회
     * - Exam과 UserExam 통합
     * - 생성일 기준 정렬
     *
     * @param limit 조회할 개수
     * @return 최근 시험 목록
     */
    List<UnifiedExamResponse> findRecentExams(int limit);

    /**
     * 시험 타입별 필터링 검색
     * - TESTWIZARD: Exam 테이블만 검색
     * - USER_CREATED: UserExam 테이블만 검색
     * - ALL: 둘 다 검색 (기본값)
     *
     * @param request 검색 조건
     * @param examType 시험 타입 (TESTWIZARD, USER_CREATED, ALL)
     * @param pageable 페이징
     * @return 필터링된 검색 결과
     */
    Page<UnifiedExamResponse> searchByType(
            ExamSearchRequest request,
            String examType,
            Pageable pageable
    );

    /**
     * 대단원(Chapter) 기준 검색
     * - Exam 테이블에서만 검색 (UserExam은 chapter 정보 없음)
     *
     * @param largeChapterCode 대단원 코드
     * @param pageable 페이징
     * @return 해당 대단원의 시험 목록
     */
    Page<UnifiedExamResponse> findByLargeChapter(Long largeChapterCode, Pageable pageable);

    /**
     * 복합 조건 고급 검색
     * - 여러 조건을 AND/OR로 조합
     * - 동적 쿼리 생성
     *
     * @param request 검색 조건
     * @param includeDeleted 삭제된 UserExam 포함 여부
     * @param pageable 페이징
     * @return 검색 결과
     */
    Page<UnifiedExamResponse> advancedSearch(
            ExamSearchRequest request,
            boolean includeDeleted,
            Pageable pageable
    );
}

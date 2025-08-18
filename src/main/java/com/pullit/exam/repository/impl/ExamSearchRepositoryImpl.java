package com.pullit.exam.repository.impl;

import com.pullit.exam.dto.request.ExamSearchRequest;
import com.pullit.exam.dto.response.ExamCountBySubjectResponse;
import com.pullit.exam.dto.response.UnifiedExamResponse;
import com.pullit.exam.entity.QExam;
import com.pullit.exam.entity.QUserExam;
import com.pullit.exam.entity.QUserExamItem;
import com.pullit.exam.enums.ExamVisibility;
import com.pullit.exam.repository.ExamSearchRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ExamSearchRepository 구현체
 * - QueryDSL을 사용한 동적 쿼리 구현
 * - Exam과 UserExam 통합 검색 로직
 */
@Repository
@RequiredArgsConstructor
public class ExamSearchRepositoryImpl implements ExamSearchRepository {

    private final JPAQueryFactory queryFactory;

    // ===== Q클래스 인스턴스 =====
    private final QExam exam = QExam.exam;
    private final QUserExam userExam = QUserExam.userExam;
    private final QUserExamItem userExamItem = QUserExamItem.userExamItem;

    /**
     * 통합 검색 메인 메서드
     */
    @Override
    public Page<UnifiedExamResponse> searchUnified(ExamSearchRequest request, Pageable pageable) {
        // ===== 1. Exam 테이블 검색 =====
        List<UnifiedExamResponse> examResults = searchFromExamTable(request);

        // ===== 2. UserExam 테이블 검색 =====
        List<UnifiedExamResponse> userExamResults = searchFromUserExamTable(request);

        // ===== 3. 결과 병합 =====
        List<UnifiedExamResponse> allResults = new ArrayList<>();
        allResults.addAll(examResults);
        allResults.addAll(userExamResults);

        // ===== 4. 정렬 적용 =====
        allResults = applySorting(allResults, pageable.getSort());

        // ===== 5. 페이징 처리 =====
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allResults.size());

        // 범위 체크
        if (start > allResults.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, allResults.size());
        }

        List<UnifiedExamResponse> pageContent = allResults.subList(start, end);

        return new PageImpl<>(pageContent, pageable, allResults.size());
    }

    /**
     * Exam 테이블에서 검색
     */
    private List<UnifiedExamResponse> searchFromExamTable(ExamSearchRequest request) {
        return queryFactory
                .select(Projections.constructor(UnifiedExamResponse.class,
                        exam.id,
                        Expressions.constant("TESTWIZARD"),  // examType
                        exam.examName,
                        exam.subject.subjectId,              // subject.subjectId 필드 사용
                        exam.subject.subjectName,                   // subjectName
                        exam.largeChapter.code,              // chapterCode (대단원)
                        exam.largeChapter.name,              // chapterName (대단원)
                        exam.subject.grade.code,             // gradeCode - Subject에서 가져옴
                        exam.subject.grade.name,             // gradeName - Subject에서 가져옴
                        exam.subject.term.code,              // termCode - Subject에서 가져옴
                        exam.subject.term.name,              // termName - Subject에서 가져옴
                        exam.subject.area.code,
                        exam.subject.area.name,
                        exam.itemCount,
                        exam.visibility,
                        exam.previewUrl,                     // pdfUrl
                        exam.createdBy,
                        exam.createdDate,
                        exam.updatedDate
                ))
                .from(exam)
                .leftJoin(exam.subject)
                .where(buildExamPredicate(request))
                .fetch();
    }

    /**
     * UserExam 테이블에서 검색
     * UserExam은 여러 과목 문제를 포함할 수 있으므로 subjectId로 직접 검색 불가
     */
    private List<UnifiedExamResponse> searchFromUserExamTable(ExamSearchRequest request) {
        BooleanBuilder builder = buildUserExamPredicate(request);

        // 과목 필터가 있는 경우 UserExamItem을 통해 검색
        if (request.getSubjectId() != null) {
            // 해당 과목 문제를 포함한 UserExam ID 먼저 조회
            List<Long> examIds = queryFactory
                    .select(userExamItem.userExam.id).distinct()
                    .from(userExamItem)
                    .where(userExamItem.subjectId.eq(request.getSubjectId()))
                    .fetch();

            if (!examIds.isEmpty()) {
                builder.and(userExam.id.in(examIds));
            } else {
                return new ArrayList<>(); // 해당 과목 문제가 없으면 빈 결과
            }
        }

        return queryFactory
                .select(Projections.constructor(UnifiedExamResponse.class,
                        userExam.id,
                        Expressions.constant("USER_CREATED"),  // examType
                        userExam.examName,
                        Expressions.nullExpression(Long.class),    // subjectId (UserExam에는 없음)
                        Expressions.nullExpression(String.class),  // subjectName (UserExam에는 없음)
                        Expressions.nullExpression(Long.class),    // chapterCode (UserExam에는 없음)
                        Expressions.nullExpression(String.class),  // chapterName (UserExam에는 없음)
                        userExam.gradeCode,                   // gradeCode
                        userExam.gradeName,                   // gradeName
                        userExam.termCode,                    // termCode
                        userExam.termName,                    // termName
                        userExam.areaCode,
                        userExam.areaName,
                        userExam.totalItems,                  // itemCount
                        userExam.visibility,
                        userExam.pdfUrl,                      // pdfUrl
                        userExam.createdBy,                   // Long 타입 userId
                        userExam.createdDate,
                        userExam.updatedDate
                ))
                .from(userExam)
                .where(
                        userExam.deletedDate.isNull(),  // Soft delete 체크
                        builder
                )
                .fetch();
    }

    /**
     * Exam 검색 조건 생성
     */
    private BooleanBuilder buildExamPredicate(ExamSearchRequest request) {
        BooleanBuilder builder = new BooleanBuilder();

        // 키워드 검색 (시험명)
        if (StringUtils.hasText(request.getKeyword())) {
            builder.and(exam.examName.containsIgnoreCase(request.getKeyword()));
        }

        // 교과서 필터 (subject.subjectId 필드 사용)
        if (request.getSubjectId() != null) {
            builder.and(exam.subject.subjectId.eq(request.getSubjectId()));
        }

        // 과목 코드 필터 (areaCode)
        if (StringUtils.hasText(request.getAreaCode())) {
            // 여러 과목 코드 지원 (콤마로 구분)
            String[] areaCodes = request.getAreaCode().split(",");
            if (areaCodes.length == 1) {
                builder.and(exam.subject.area.code.eq(areaCodes[0].trim()));
            } else {
                builder.and(exam.subject.area.code.in(areaCodes));
            }
        }

        // 학년 필터 추가 (Subject의 grade.code 사용)
        if (StringUtils.hasText(request.getGradeCode())) {
            // 여러 학년 코드 지원 (콤마로 구분)
            String[] gradeCodes = request.getGradeCode().split(",");
            if (gradeCodes.length == 1) {
                builder.and(exam.subject.grade.code.eq(gradeCodes[0].trim()));
            } else {
                builder.and(exam.subject.grade.code.in(gradeCodes));
            }
        }

        // 학기 필터 추가 (Subject의 term.code 사용)
        if (StringUtils.hasText(request.getTermCode())) {
            // 여러 학기 코드 지원 (콤마로 구분)
            String[] termCodes = request.getTermCode().split(",");
            if (termCodes.length == 1) {
                builder.and(exam.subject.term.code.eq(termCodes[0].trim()));
            } else {
                builder.and(exam.subject.term.code.in(termCodes));
            }
        }

        // 대단원 필터 (Exam만 가능) - 여러 대단원 지원
        if (StringUtils.hasText(request.getLargeChapterCode())) {
            String largeChapterStr = request.getLargeChapterCode();
            if (largeChapterStr.contains(",")) {
                // 여러 대단원 코드
                String[] chapterCodes = largeChapterStr.split(",");
                List<Long> chapterCodesList = new ArrayList<>();
                for (String code : chapterCodes) {
                    try {
                        chapterCodesList.add(Long.parseLong(code.trim()));
                    } catch (NumberFormatException e) {
                        // 숫자가 아닌 경우 무시
                    }
                }
                if (!chapterCodesList.isEmpty()) {
                    builder.and(exam.largeChapter.code.in(chapterCodesList));
                }
            } else {
                // 단일 대단원 코드
                try {
                    Long chapterCode = Long.parseLong(largeChapterStr);
                    builder.and(exam.largeChapter.code.eq(chapterCode));
                } catch (NumberFormatException e) {
                    // 숫자가 아닌 경우 무시
                }
            }
        }

        // 공개 여부
        if (request.getVisibility() != null) {
            builder.and(exam.visibility.eq(request.getVisibility()));
        }

        // 생성자 필터 (Long 타입 userId)
        if (request.getCreatedBy() != null) {
            builder.and(exam.createdBy.eq(request.getCreatedBy()));
        }

        // 날짜 범위 (생성일 기준)
        if (request.getStartDate() != null && request.getEndDate() != null) {
            builder.and(exam.createdDate.between(
                    request.getStartDate(),
                    request.getEndDate()
            ));
        }

        return builder;
    }

    /**
     * UserExam 검색 조건 생성
     * UserExam은 과목 필터를 제외한 조건만 처리 (과목은 별도 처리)
     */
    private BooleanBuilder buildUserExamPredicate(ExamSearchRequest request) {
        BooleanBuilder builder = new BooleanBuilder();

        // 키워드 검색 (시험명)
        if (StringUtils.hasText(request.getKeyword())) {
            builder.and(userExam.examName.containsIgnoreCase(request.getKeyword()));
        }

        // 학년 필터
        if (StringUtils.hasText(request.getGradeCode())) {
            builder.and(userExam.gradeCode.eq(request.getGradeCode()));
        }

        // 학기 필터
        if (StringUtils.hasText(request.getTermCode())) {
            builder.and(userExam.termCode.eq(request.getTermCode()));
        }

        // 공개 여부
        if (request.getVisibility() != null) {
            builder.and(userExam.visibility.eq(request.getVisibility()));
        }

        // 생성자 필터 (Long 타입 userId)
        if (request.getCreatedBy() != null) {
            builder.and(userExam.createdBy.eq(request.getCreatedBy()));
        }

        // 날짜 범위 (생성일 기준)
        if (request.getStartDate() != null && request.getEndDate() != null) {
            builder.and(userExam.createdDate.between(
                    request.getStartDate(),
                    request.getEndDate()
            ));
        }

        return builder;
    }

    /**
     * 빠른 검색 (자동완성용)
     */
    @Override
    public List<UnifiedExamResponse> quickSearch(String keyword, int limit) {
        if (!StringUtils.hasText(keyword)) {
            return new ArrayList<>();
        }

        // 간단한 검색 조건 생성
        ExamSearchRequest request = ExamSearchRequest.builder()
                .keyword(keyword)
                .build();

        // 두 테이블에서 검색
        List<UnifiedExamResponse> results = new ArrayList<>();
        results.addAll(searchFromExamTable(request));
        results.addAll(searchFromUserExamTable(request));

        // 생성일 기준 정렬 후 제한
        return results.stream()
                .sorted((a, b) -> b.getCreatedDate().compareTo(a.getCreatedDate()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 사용자 접근 가능한 시험 조회
     */
    @Override
    public Page<UnifiedExamResponse> findAccessibleExams(Long userId, Pageable pageable) {
        // ===== Exam 테이블: 본인 생성 또는 PUBLIC =====
        List<UnifiedExamResponse> examResults = queryFactory
                .select(Projections.constructor(UnifiedExamResponse.class,
                        exam.id,
                        Expressions.constant("TESTWIZARD"),
                        exam.examName,
                        exam.subject.subjectId,
                        exam.subject.subjectName,
                        exam.largeChapter.code,
                        exam.largeChapter.name,
                        exam.subject.grade.code,  // gradeCode - Subject에서 가져옴
                        exam.subject.grade.name,  // gradeName - Subject에서 가져옴
                        exam.subject.term.code,   // termCode - Subject에서 가져옴
                        exam.subject.term.name,   // termName - Subject에서 가져옴
                        exam.itemCount,
                        exam.visibility,
                        exam.previewUrl,
                        exam.createdBy,
                        exam.createdDate,
                        exam.updatedDate
                ))
                .from(exam)
                .leftJoin(exam.subject)
                .where(
                        exam.createdBy.eq(userId)
                                .or(exam.visibility.eq(ExamVisibility.PUBLIC))
                )
                .fetch();

        // ===== UserExam 테이블: 본인 생성 또는 PUBLIC =====
        List<UnifiedExamResponse> userExamResults = queryFactory
                .select(Projections.constructor(UnifiedExamResponse.class,
                        userExam.id,
                        Expressions.constant("USER_CREATED"),
                        userExam.examName,
                        Expressions.nullExpression(Long.class),
                        Expressions.nullExpression(String.class),
                        Expressions.nullExpression(Long.class),
                        Expressions.nullExpression(String.class),
                        userExam.gradeCode,
                        userExam.gradeName,
                        userExam.termCode,
                        userExam.termName,
                        userExam.totalItems,
                        userExam.visibility,
                        userExam.pdfUrl,
                        userExam.createdBy,
                        userExam.createdDate,
                        userExam.updatedDate
                ))
                .from(userExam)
                .where(
                        userExam.deletedDate.isNull(),
                        userExam.createdBy.eq(userId)
                                .or(userExam.visibility.eq(ExamVisibility.PUBLIC))
                )
                .fetch();

        // 결과 병합 및 페이징
        List<UnifiedExamResponse> allResults = new ArrayList<>();
        allResults.addAll(examResults);
        allResults.addAll(userExamResults);

        // 정렬 및 페이징 처리
        allResults.sort((a, b) -> b.getCreatedDate().compareTo(a.getCreatedDate()));

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allResults.size());

        List<UnifiedExamResponse> pageContent = allResults.subList(start, end);

        return new PageImpl<>(pageContent, pageable, allResults.size());
    }

    /**
     * 최근 생성된 시험 조회
     */
    @Override
    public List<UnifiedExamResponse> findRecentExams(int limit) {
        // Exam 최근 시험
        List<UnifiedExamResponse> recentExams = queryFactory
                .select(Projections.constructor(UnifiedExamResponse.class,
                        exam.id,
                        Expressions.constant("TESTWIZARD"),
                        exam.examName,
                        exam.subject.subjectId,
                        exam.subject.subjectName,
                        exam.largeChapter.code,
                        exam.largeChapter.name,
                        exam.subject.grade.code,  // gradeCode - Subject에서 가져옴
                        exam.subject.grade.name,  // gradeName - Subject에서 가져옴
                        exam.subject.term.code,   // termCode - Subject에서 가져옴
                        exam.subject.term.name,   // termName - Subject에서 가져옴
                        exam.itemCount,
                        exam.visibility,
                        exam.previewUrl,
                        exam.createdBy,
                        exam.createdDate,
                        exam.updatedDate
                ))
                .from(exam)
                .leftJoin(exam.subject)
                .orderBy(exam.createdDate.desc())
                .limit(limit)
                .fetch();

        // UserExam 최근 시험
        List<UnifiedExamResponse> recentUserExams = queryFactory
                .select(Projections.constructor(UnifiedExamResponse.class,
                        userExam.id,
                        Expressions.constant("USER_CREATED"),
                        userExam.examName,
                        Expressions.nullExpression(Long.class),
                        Expressions.nullExpression(String.class),
                        Expressions.nullExpression(Long.class),
                        Expressions.nullExpression(String.class),
                        userExam.gradeCode,
                        userExam.gradeName,
                        userExam.termCode,
                        userExam.termName,
                        userExam.totalItems,
                        userExam.visibility,
                        userExam.pdfUrl,
                        userExam.createdBy,
                        userExam.createdDate,
                        userExam.updatedDate
                ))
                .from(userExam)
                .where(userExam.deletedDate.isNull())
                .orderBy(userExam.createdDate.desc())
                .limit(limit)
                .fetch();

        // 병합 후 다시 정렬
        List<UnifiedExamResponse> allResults = new ArrayList<>();
        allResults.addAll(recentExams);
        allResults.addAll(recentUserExams);

        return allResults.stream()
                .sorted((a, b) -> b.getCreatedDate().compareTo(a.getCreatedDate()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 시험 타입별 검색
     */
    @Override
    public Page<UnifiedExamResponse> searchByType(
            ExamSearchRequest request,
            String examType,
            Pageable pageable) {

        List<UnifiedExamResponse> results = new ArrayList<>();

        // TESTWIZARD 타입만 검색
        if ("TESTWIZARD".equals(examType)) {
            results = searchFromExamTable(request);
        }
        // USER_CREATED 타입만 검색
        else if ("USER_CREATED".equals(examType)) {
            results = searchFromUserExamTable(request);
        }
        // 모두 검색 (기본값)
        else {
            results.addAll(searchFromExamTable(request));
            results.addAll(searchFromUserExamTable(request));
        }

        // 정렬 적용
        results = applySorting(results, pageable.getSort());

        // 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), results.size());

        List<UnifiedExamResponse> pageContent = results.subList(start, end);

        return new PageImpl<>(pageContent, pageable, results.size());
    }

    /**
     * 대단원 기준 검색 (Exam만)
     */
    @Override
    public Page<UnifiedExamResponse> findByLargeChapter(Long largeChapterCode, Pageable pageable) {
        ExamSearchRequest request = ExamSearchRequest.builder()
                .largeChapterCode(String.valueOf(largeChapterCode))
                .build();

        // Exam 테이블에서만 검색
        List<UnifiedExamResponse> results = searchFromExamTable(request);

        // 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), results.size());

        List<UnifiedExamResponse> pageContent = results.subList(start, end);

        return new PageImpl<>(pageContent, pageable, results.size());
    }

    /**
     * 고급 검색
     */
    @Override
    public Page<UnifiedExamResponse> advancedSearch(
            ExamSearchRequest request,
            boolean includeDeleted,
            Pageable pageable) {

        // 기본 검색과 동일하지만 삭제된 항목 포함 여부 처리
        // includeDeleted가 true면 UserExam의 deletedDate 조건 제외

        // 구현 내용은 searchUnified와 유사
        // includeDeleted 파라미터에 따라 조건 변경

        return searchUnified(request, pageable);  // 임시 구현
    }

    /**
     * 과목별 시험 개수 집계
     */
    @Override
    public List<ExamCountBySubjectResponse> countExamsBySubject(Long subjectId) {
        // 구현 필요
        // GROUP BY 쿼리로 과목별 집계
        return new ArrayList<>();
    }

    /**
     * 정렬 적용 헬퍼 메서드
     */
    private List<UnifiedExamResponse> applySorting(List<UnifiedExamResponse> results, Sort sort) {
        if (sort.isEmpty()) {
            // 기본 정렬: 생성일 내림차순
            return results.stream()
                    .sorted((a, b) -> b.getCreatedDate().compareTo(a.getCreatedDate()))
                    .collect(Collectors.toList());
        }

        // Sort 객체의 정렬 조건 적용
        for (Sort.Order order : sort) {
            String property = order.getProperty();
            boolean ascending = order.isAscending();

            // 정렬 필드에 따라 Comparator 적용
            if ("createdDate".equals(property)) {
                results.sort((a, b) -> ascending ?
                        a.getCreatedDate().compareTo(b.getCreatedDate()) :
                        b.getCreatedDate().compareTo(a.getCreatedDate()));
            } else if ("examName".equals(property)) {
                results.sort((a, b) -> ascending ?
                        a.getExamName().compareTo(b.getExamName()) :
                        b.getExamName().compareTo(a.getExamName()));
            }
            // 필요한 다른 정렬 필드 추가
        }

        return results;
    }
}
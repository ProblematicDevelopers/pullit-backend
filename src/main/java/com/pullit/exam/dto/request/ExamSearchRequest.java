package com.pullit.exam.dto.request;

import com.pullit.exam.enums.ExamVisibility;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSearchRequest {
    // ===== 공통 검색 조건 =====

    /**
     * 검색 키워드 (시험명 검색)
     */
    private String keyword;

    /**
     * 과목 ID
     */
    private Long subjectId;

    /**
     * 공개 여부 (PRIVATE, SCHOOL, PUBLIC)
     */
    private ExamVisibility visibility;

    /**
     * 생성자 username
     */
    private Long createdBy;

    /**
     * 시작 날짜 (생성일 기준)
     */
    private LocalDateTime startDate;

    /**
     * 종료 날짜 (생성일 기준)
     */
    private LocalDateTime endDate;

    // ===== Exam 전용 검색 조건 =====

    /**
     * 대단원 코드 (Exam만 해당)
     */
    private Long largeChapterCode;

    // ===== UserExam 전용 검색 조건 =====

    /**
     * 학년 코드 (UserExam만 해당)
     */
    private String gradeCode;

    /**
     * 학기 코드 (UserExam만 해당)
     */
    private String termCode;

    /**
     * 지역 코드 (UserExam만 해당)
     */
    private String areaCode;

    // ===== 추가 옵션 =====

    /**
     * 시험 타입 필터 (TESTWIZARD, USER_CREATED, ALL)
     * null이면 ALL로 처리
     */
    private String examType;

    /**
     * 삭제된 UserExam 포함 여부 (기본값: false)
     */
    @Builder.Default
    private boolean includeDeleted = false;

    /**
     * 검색 결과 정렬 필드
     * (createdDate, examName, itemCount 등)
     */
    private String sortBy;

    /**
     * 정렬 방향 (ASC, DESC)
     */
    @Builder.Default
    private String sortDirection = "DESC";

    // ===== 유효성 검증 메서드 =====

    /**
     * 검색 조건이 있는지 확인
     */
    public boolean hasSearchCondition() {
        return keyword != null ||
                subjectId != null ||
                visibility != null ||
                createdBy != null ||
                startDate != null ||
                largeChapterCode != null ||
                gradeCode != null ||
                termCode != null;
    }

    /**
     * 날짜 범위가 유효한지 확인
     */
    public boolean hasValidDateRange() {
        return startDate != null && endDate != null && !startDate.isAfter(endDate);
    }
}

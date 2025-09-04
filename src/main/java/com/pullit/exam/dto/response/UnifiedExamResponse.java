package com.pullit.exam.dto.response;

import com.pullit.exam.enums.ExamVisibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UnifiedExamResponse {
    
    /**
     * QueryDSL Projections 전용 생성자 (19개 매개변수)
     * ExamSearchRepositoryImpl의 searchFromExamTable() 메서드에서 사용
     */
    public UnifiedExamResponse(
            Long id,
            String examType,
            String examName,
            Long subjectId,
            String subjectName,
            Long chapterCode,
            String chapterName,
            String gradeCode,
            String gradeName,
            String termCode,
            String termName,
            String areaCode,
            String areaName,
            Integer itemCount,
            ExamVisibility visibility,
            String pdfUrl,
            Long createdBy,  // Long 타입으로 수정
            LocalDateTime createdDate,
            LocalDateTime updatedDate
    ) {
        this.id = id;
        this.examType = examType;
        this.examName = examName;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.chapterCode = chapterCode;
        this.chapterName = chapterName;
        this.gradeCode = gradeCode;
        this.gradeName = gradeName;
        this.termCode = termCode;
        this.termName = termName;
        this.areaCode = areaCode;
        this.areaName = areaName;
        this.itemCount = itemCount;
        this.visibility = visibility;
        this.pdfUrl = pdfUrl;
        this.createdBy = createdBy != null ? String.valueOf(createdBy) : null;  // Long을 String으로 변환
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        // UserExam 전용 필드들은 null로 초기화
        this.answerPdfUrl = null;
        this.totalPoints = null;
        this.timeLimit = null;
        this.examDate = null;
        this.userExamType = null;
    }
    
    /**
     * QueryDSL Projections 전용 생성자 - UserExam용 (20개 매개변수)
     * ExamSearchRepositoryImpl의 searchFromUserExamTable() 메서드에서 사용
     */
    public UnifiedExamResponse(
            Long id,
            String examType,
            String examName,
            Long subjectId,
            String subjectName,
            Long chapterCode,
            String chapterName,
            String gradeCode,
            String gradeName,
            String termCode,
            String termName,
            String areaCode,
            String areaName,
            Integer itemCount,
            ExamVisibility visibility,
            String pdfUrl,
            Long createdBy,
            LocalDateTime createdDate,
            LocalDateTime updatedDate,
            String userExamType  // UserExam의 실제 시험 타입
    ) {
        this(id, examType, examName, subjectId, subjectName, chapterCode, chapterName,
             gradeCode, gradeName, termCode, termName, areaCode, areaName,
             itemCount, visibility, pdfUrl, createdBy, createdDate, updatedDate);
        this.userExamType = userExamType;
    }
    // ===== 기본 정보 =====

    /**
     * 시험 ID
     */
    private Long id;

    /**
     * 시험 타입 (TESTWIZARD, USER_CREATED)
     */
    private String examType;

    /**
     * 시험명
     */
    private String examName;

    // ===== 과목 정보 =====

    /**
     * 과목 ID
     */
    private Long subjectId;

    /**
     * 과목명
     */
    private String subjectName;

    // ===== Chapter 정보 (Exam만) =====

    /**
     * 대단원 코드 (Exam만 해당, UserExam은 null)
     */
    private Long chapterCode;

    /**
     * 대단원명 (Exam만 해당, UserExam은 null)
     */
    private String chapterName;

    // ===== 학년/학기 정보 (UserExam만) =====

    /**
     * 학년 코드 (UserExam만 해당, Exam은 null)
     */
    private String gradeCode;

    /**
     * 학년명 (UserExam만 해당, Exam은 null)
     */
    private String gradeName;

    /**
     * 학기 코드 (UserExam만 해당, Exam은 null)
     */
    private String termCode;

    /**
     * 학기명 (UserExam만 해당, Exam은 null)
     */
    private String termName;

    // ===== 시험 상세 정보 =====

    /**
     * 문항 수
     */
    private Integer itemCount;

    /**
     * 공개 여부
     */
    private ExamVisibility visibility;

    /**
     * PDF URL (미리보기용)
     */
    private String pdfUrl;

    /**
     * 답안 PDF URL (UserExam만)
     */
    private String answerPdfUrl;

    // ===== 메타 정보 =====

    /**
     * 생성자
     */
    private String createdBy;

    /**
     * 생성일시
     */
    private LocalDateTime createdDate;

    /**
     * 수정일시
     */
    private LocalDateTime updatedDate;

    // ===== 추가 정보 =====

    /**
     * 총점 (UserExam만)
     */
    private Integer totalPoints;

    /**
     * 시험 시간 제한 (분 단위, UserExam만)
     */
    private Integer timeLimit;

    /**
     * 시험 날짜 (UserExam만)
     */
    private LocalDateTime examDate;

    /**
     * area(과목)정보 추가
     */
    private String areaCode;
    private String areaName;
    
    /**
     * UserExam의 실제 시험 타입 (CBT, PAPER 등)
     * UserExam만 해당, Exam은 null
     */
    private String userExamType;
    // ===== 헬퍼 메서드 =====

    /**
     * TestWizard 시험인지 확인
     */
    @JsonIgnore
    public boolean isTestWizardExam() {
        return "TESTWIZARD".equals(examType);
    }

    /**
     * 사용자 생성 시험인지 확인
     */
    @JsonIgnore
    public boolean isUserCreatedExam() {
        return "USER_CREATED".equals(examType);
    }

    /**
     * 공개 시험인지 확인
     */
    @JsonIgnore
    public boolean isPublic() {
        return ExamVisibility.PUBLIC.equals(visibility);
    }

    /**
     * 학교 공개 시험인지 확인
     */
    @JsonIgnore
    public boolean isSchoolVisible() {
        return ExamVisibility.SCHOOL.equals(visibility);
    }

    /**
     * 비공개 시험인지 확인
     */
    @JsonIgnore
    public boolean isPrivate() {
        return ExamVisibility.PRIVATE.equals(visibility);
    }

    /**
     * 표시용 시험 타입 텍스트
     */
    @JsonIgnore
    public String getExamTypeText() {
        if (isTestWizardExam()) {
            return "테스트위자드";
        } else if (isUserCreatedExam()) {
            return "사용자 생성";
        }
        return "알 수 없음";
    }

    /**
     * 표시용 공개 범위 텍스트
     */
    @JsonIgnore
    public String getVisibilityText() {
        if (visibility == null) return "비공개";

        switch (visibility) {
            case PUBLIC:
                return "전체 공개";
            case SCHOOL:
                return "학교 공개";
            case PRIVATE:
                return "비공개";
            default:
                return "비공개";
        }
    }

    /**
     * Backward-compatibility for cached JSON that accidentally included a boolean property named "public"
     * due to a previous boolean getter (isPublic). Accept and ignore this field on deserialization.
     */
    @JsonProperty("public")
    public void setPublicFlag(Boolean ignored) {
        // no-op: kept only to consume legacy cached field
    }

    @JsonProperty("private")
    public void setPrivateFlag(Boolean ignored) {
        // no-op
    }

    @JsonProperty("schoolVisible")
    public void setSchoolVisibleFlag(Boolean ignored) {
        // no-op
    }
}

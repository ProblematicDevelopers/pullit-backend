package com.pullit.pdf.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "PDF 내보내기 요청 정보")
public class PdfExportRequest {

    @NotNull(message = "시험지 ID는 필수입니다")
    @Positive(message = "시험지 ID는 양수여야 합니다")
    @Schema(description = "시험지 ID", example = "123", required = true)
    @JsonProperty("examId")
    private Long examId;

    @Schema(description = "PDF 템플릿 ID (미지정시 기본 템플릿)", example = "1")
    @JsonProperty("templateId")
    private Long templateId;

    @NotNull(message = "출력 형식은 필수입니다")
    @Schema(description = "출력 형식", example = "COMBINED", required = true)
    @JsonProperty("exportType")
    private ExportType exportType = ExportType.COMBINED;

    @Size(max = 100, message = "파일명은 100자를 초과할 수 없습니다")
    @Pattern(regexp = "^[a-zA-Z0-9가-힣_\\-\\s]+$",
            message = "파일명에 특수문자는 사용할 수 없습니다")
    @Schema(description = "PDF 파일명 (확장자 제외)", example = "2024년_1학기_중간고사")
    @JsonProperty("fileName")
    private String fileName;

    @Valid
    @Schema(description = "PDF 헤더 설정")
    @JsonProperty("headerOptions")
    private HeaderOptions headerOptions;

    @Valid
    @Schema(description = "PDF 푸터 설정")
    @JsonProperty("footerOptions")
    private FooterOptions footerOptions;

    @Valid
    @Schema(description = "문제 표시 옵션")
    @JsonProperty("questionOptions")
    private QuestionOptions questionOptions;

    @Valid
    @Schema(description = "답안 표시 옵션")
    @JsonProperty("answerOptions")
    private AnswerOptions answerOptions;

    @Valid
    @Schema(description = "페이지 설정")
    @JsonProperty("pageOptions")
    private PageOptions pageOptions;

    @Schema(description = "워터마크 텍스트", example = "SAMPLE")
    @JsonProperty("watermark")
    private String watermark;

    @Schema(description = "PDF 보안 설정")
    @JsonProperty("securityOptions")
    private SecurityOptions securityOptions;

    @Schema(description = "커스텀 데이터 (key-value)")
    @JsonProperty("customData")
    private Map<String, Object> customData;

    /**
     * 헤더 옵션 내부 클래스
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HeaderOptions {
        @Schema(description = "제목 표시", example = "true")
        private Boolean showTitle = true;

        @Schema(description = "학교명 표시", example = "true")
        private Boolean showSchool = true;

        @Schema(description = "시험 날짜 표시", example = "true")
        private Boolean showDate = true;

        @Schema(description = "학년/반 표시", example = "true")
        private Boolean showGradeClass = true;

        @Schema(description = "커스텀 헤더 텍스트", example = "2024학년도 1학기")
        private String customText;

        @Schema(description = "로고 이미지 URL")
        private String logoUrl;
    }

    /**
     * 푸터 옵션 내부 클래스
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FooterOptions {
        @Schema(description = "페이지 번호 표시", example = "true")
        private Boolean showPageNumber = true;

        @Schema(description = "총 페이지 수 표시", example = "true")
        private Boolean showTotalPages = true;

        @Schema(description = "생성 일시 표시", example = "false")
        private Boolean showGeneratedDate = false;

        @Schema(description = "커스텀 푸터 텍스트")
        private String customText;
    }

    /**
     * 문제 표시 옵션 내부 클래스
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionOptions {
        @Schema(description = "문제 번호 표시", example = "true")
        private Boolean showQuestionNumber = true;

        @Schema(description = "배점 표시", example = "true")
        private Boolean showPoints = true;

        @Schema(description = "난이도 표시", example = "false")
        private Boolean showDifficulty = false;

        @Schema(description = "출제 범위 표시", example = "false")
        private Boolean showChapter = false;

        @Schema(description = "문제당 최소 높이 (mm)", example = "50")
        @Min(20)
        @Max(200)
        private Integer minHeight = 50;

        @Schema(description = "이미지 최대 너비 (mm)", example = "150")
        @Min(50)
        @Max(200)
        private Integer maxImageWidth = 150;
    }

    /**
     * 답안 표시 옵션 내부 클래스
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnswerOptions {
        @Schema(description = "정답 표시", example = "true")
        private Boolean showCorrectAnswer = true;

        @Schema(description = "해설 표시", example = "true")
        private Boolean showExplanation = true;

        @Schema(description = "채점 기준 표시", example = "false")
        private Boolean showScoringCriteria = false;

        @Schema(description = "답안 작성 공간 (주관식)", example = "true")
        private Boolean showAnswerSpace = true;

        @Schema(description = "답안 공간 높이 (mm)", example = "30")
        @Min(10)
        @Max(100)
        private Integer answerSpaceHeight = 30;
    }

    /**
     * 페이지 설정 내부 클래스
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PageOptions {
        @Schema(description = "페이지 크기", example = "A4")
        private String pageSize = "A4";

        @Schema(description = "페이지 방향 (PORTRAIT/LANDSCAPE)", example = "PORTRAIT")
        private String orientation = "PORTRAIT";

        @Schema(description = "상단 여백 (mm)", example = "20")
        @Min(5)
        @Max(50)
        private Integer marginTop = 20;

        @Schema(description = "하단 여백 (mm)", example = "20")
        @Min(5)
        @Max(50)
        private Integer marginBottom = 20;

        @Schema(description = "좌측 여백 (mm)", example = "15")
        @Min(5)
        @Max(50)
        private Integer marginLeft = 15;

        @Schema(description = "우측 여백 (mm)", example = "15")
        @Min(5)
        @Max(50)
        private Integer marginRight = 15;

        @Schema(description = "단 구분 (1단/2단)", example = "1")
        @Min(1)
        @Max(2)
        private Integer columns = 1;
    }

    /**
     * 보안 옵션 내부 클래스
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SecurityOptions {
        @Schema(description = "인쇄 허용", example = "true")
        private Boolean allowPrint = true;

        @Schema(description = "복사 허용", example = "false")
        private Boolean allowCopy = false;

        @Schema(description = "편집 허용", example = "false")
        private Boolean allowEdit = false;

        @Schema(description = "암호 설정")
        private String password;
    }

    /**
     * 요청 검증 메서드
     * 비즈니스 로직 검증용
     */
    public boolean isValid() {
        // ANSWER_ONLY 타입일 때는 answerOptions가 필수
        if (exportType == ExportType.ANSWER_ONLY && answerOptions == null) {
            return false;
        }

        // QUESTION_ONLY 타입일 때는 questionOptions가 필수
        if (exportType == ExportType.QUESTION_ONLY && questionOptions == null) {
            return false;
        }

        return true;
    }

    /**
     * 기본값 설정 메서드
     * null인 옵션들에 기본값을 설정합니다
     */
    public void applyDefaults() {
        if (headerOptions == null) {
            headerOptions = HeaderOptions.builder()
                    .showTitle(true)
                    .showSchool(true)
                    .showDate(true)
                    .build();
        }

        if (footerOptions == null) {
            footerOptions = FooterOptions.builder()
                    .showPageNumber(true)
                    .showTotalPages(true)
                    .build();
        }

        if (pageOptions == null) {
            pageOptions = PageOptions.builder()
                    .pageSize("A4")
                    .orientation("PORTRAIT")
                    .marginTop(20)
                    .marginBottom(20)
                    .marginLeft(15)
                    .marginRight(15)
                    .columns(1)
                    .build();
        }
    }

}

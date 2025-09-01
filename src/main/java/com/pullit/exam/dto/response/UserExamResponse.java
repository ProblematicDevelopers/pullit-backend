package com.pullit.exam.dto.response;

import com.pullit.exam.entity.UserExam;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserExamResponse {
    private Long id;
    private String examName;
    private String gradeCode;
    private String gradeName;
    private String termCode;
    private String termName;
    private String areaCode;
    private String areaName;
    private String examType;
    private Integer totalItems;
    private Integer totalPoints;
    private String pdfUrl;
    private String answerPdfUrl;
    private LocalDateTime pdfGeneratedAt;
    private Integer timeLimit;
    private LocalDate examDate;
    private String description;
    private String visibility;
    private Long classId;
    private List<UserExamItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserExamItemResponse {
        private Long id;
        private Long itemId;
        private Long subjectId;
        private Integer itemOrder;
        private Integer points;
    }

    public static UserExamResponse from(UserExam exam) {
        return UserExamResponse.builder()
                .id(exam.getId())
                .examName(exam.getExamName())
                .gradeCode(exam.getGradeCode())
                .gradeName(exam.getGradeName())
                .termCode(exam.getTermCode())
                .termName(exam.getTermName())
                .areaCode(exam.getAreaCode())
                .areaName(exam.getAreaName())
                .examType(exam.getExamType())
                .totalItems(exam.getTotalItems())
                .totalPoints(exam.getTotalPoints())
                .pdfUrl(exam.getPdfUrl())
                .answerPdfUrl(exam.getAnswerPdfUrl())
                .pdfGeneratedAt(exam.getPdfGeneratedAt())
                .timeLimit(exam.getTimeLimit())
                .examDate(exam.getExamDate())
                .description(exam.getDescription())
                .visibility(exam.getVisibility() != null ? exam.getVisibility().name() : null)
                .classId(exam.getClassId())
                .items(exam.getExamItems().stream()
                        .map(item -> UserExamItemResponse.builder()
                                .id(item.getId())
                                .itemId(item.getItemId())
                                .subjectId(item.getSubjectId())
                                .itemOrder(item.getItemOrder())
                                .points(item.getPoints())
                                .build())
                        .collect(Collectors.toList()))
                .createdAt(exam.getCreatedDate())
                .updatedAt(exam.getUpdatedDate())
                .build();
    }
}

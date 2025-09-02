package com.pullit.exam.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pullit.exam.entity.TeacherLiveExam;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeacherLiveExamResponse {
    
    private Long id;
    private String examName;
    private Long classId;
    private String className;
    private Long teacherId;
    private String teacherName;
    private String examType;
    private String examStatus;
    private Integer totalItems;
    private Integer totalPoints;
    private Integer timeLimit;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private LocalDateTime scheduledDateTime;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String description;
    private String gradeCode;
    private String termCode;
    private String subjectCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 문제 목록 (필요시 포함)
    private List<TeacherLiveExamItemResponse> examItems;
    
    // WebSocket 이벤트 타입
    private String eventType;
    
    // 원본 사용자 시험 ID (학생 라우팅에 사용)
    private Long examId;
    
    // 추가 상태 정보
    private boolean canStart;
    private boolean canTake;
    private boolean isActive;
    
    public static TeacherLiveExamResponse from(TeacherLiveExam exam) {
        TeacherLiveExamResponseBuilder builder = TeacherLiveExamResponse.builder()
                .id(exam.getId())
                .examName(exam.getExamName())
                .classId(exam.getExamClass().getClassId())
                .className(exam.getExamClass().getClassName())
                .teacherId(exam.getTeacher().getId())
                .teacherName(exam.getTeacher().getFullName())
                .examType(exam.getExamType())
                .examStatus(exam.getExamStatus().name())
                .totalItems(exam.getTotalItems())
                .totalPoints(exam.getTotalPoints())
                .timeLimit(exam.getTimeLimit())
                .scheduledDate(exam.getScheduledDate())
                .scheduledTime(exam.getScheduledTime())
                .scheduledDateTime(exam.getScheduledDateTime())
                .startedAt(exam.getStartedAt())
                .endedAt(exam.getEndedAt())
                .description(exam.getDescription())
                .gradeCode(exam.getGradeCode())
                .termCode(exam.getTermCode())
                .subjectCode(exam.getSubjectCode())
                .createdAt(exam.getCreatedDate())
                .updatedAt(exam.getUpdatedDate())
                .canStart(exam.canStart())
                .canTake(exam.canTake())
                .isActive(exam.isActive());
        
        // 문제 목록 포함 (lazy loading 주의)
        if (exam.getExamItems() != null && !exam.getExamItems().isEmpty()) {
            // 첫 문제에서 원본 시험 ID 유추
            try {
                com.pullit.exam.entity.UserExamItem first = exam.getExamItems().get(0).getUserExamItem();
                if (first != null && first.getUserExam() != null) {
                    builder.examId(first.getUserExam().getId());
                }
            } catch (Exception ignored) {}
            builder.examItems(exam.getExamItems().stream()
                    .map(TeacherLiveExamItemResponse::from)
                    .collect(Collectors.toList()));
        }
        
        return builder.build();
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherLiveExamItemResponse {
        private Long id;
        private Integer itemOrder;
        private Integer points;
        private String questionText;
        private String questionHtml;
        
        public static TeacherLiveExamItemResponse from(com.pullit.exam.entity.TeacherLiveExamItem item) {
            return TeacherLiveExamItemResponse.builder()
                    .id(item.getId())
                    .itemOrder(item.getItemOrder())
                    .points(item.getPoints())
                    .questionText(item.getQuestionText())
                    .questionHtml(item.getQuestionHtml())
                    .build();
        }
    }
}

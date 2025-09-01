package com.pullit.exam.dto.response;

import com.pullit.exam.entity.ExamAssignment;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 시험 출제 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamAssignmentResponse {

    private Long assignmentId;
    private Long examId;
    private String examName;
    private List<ClassInfo> assignedClasses;
    private LocalDate examDate;
    private LocalTime examTime;
    private Integer timeLimit;
    private LocalDateTime examStartDateTime;
    private LocalDateTime examEndDateTime;
    private Boolean notificationSent;
    private LocalDateTime notificationSentAt;
    private String status;
    private Integer totalStudents;
    private String message;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClassInfo {
        private Long classId;
        private String className;
        private String grade;
        private Integer studentCount;
        private Long assignmentId;
    }

    /**
     * 단일 ExamAssignment를 Response로 변환
     */
    public static ExamAssignmentResponse from(ExamAssignment assignment) {
        return ExamAssignmentResponse.builder()
                .assignmentId(assignment.getId())
                .examId(assignment.getUserExam().getId())
                .examName(assignment.getUserExam().getExamName())
                .examDate(assignment.getExamDate())
                .examTime(assignment.getExamTime())
                .timeLimit(assignment.getTimeLimit())
                .examStartDateTime(assignment.getExamStartDateTime())
                .examEndDateTime(assignment.getExamEndDateTime())
                .notificationSent(assignment.getNotificationSent())
                .notificationSentAt(assignment.getNotificationSentAt())
                .status(assignment.getStatus().name())
                .build();
    }

    /**
     * 여러 ExamAssignment를 하나의 Response로 변환 (동일 시험, 여러 학급)
     */
    public static ExamAssignmentResponse fromMultiple(List<ExamAssignment> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return null;
        }

        ExamAssignment first = assignments.get(0);
        List<ClassInfo> classInfos = assignments.stream()
                .map(assignment -> ClassInfo.builder()
                        .classId(assignment.getClassEntity().getClassId())
                        .className(assignment.getClassEntity().getClassName())
                        .grade(assignment.getClassEntity().getClassGrade() != null 
                            ? assignment.getClassEntity().getClassGrade().getCode() : null)
                        .studentCount(0) // Will be calculated from Student repository
                        .assignmentId(assignment.getId())
                        .build())
                .collect(Collectors.toList());

        int totalStudents = classInfos.stream()
                .mapToInt(ClassInfo::getStudentCount)
                .sum();

        return ExamAssignmentResponse.builder()
                .examId(first.getUserExam().getId())
                .examName(first.getUserExam().getExamName())
                .assignedClasses(classInfos)
                .examDate(first.getExamDate())
                .examTime(first.getExamTime())
                .timeLimit(first.getTimeLimit())
                .examStartDateTime(first.getExamStartDateTime())
                .examEndDateTime(first.getExamEndDateTime())
                .notificationSent(first.getNotificationSent())
                .status(first.getStatus().name())
                .totalStudents(totalStudents)
                .message(String.format("%d개 학급에 시험이 성공적으로 출제되었습니다.", classInfos.size()))
                .build();
    }
}
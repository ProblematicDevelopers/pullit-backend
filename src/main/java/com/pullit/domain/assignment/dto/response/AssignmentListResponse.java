package com.pullit.domain.assignment.dto.response;

import com.pullit.domain.assignment.entity.Assignment;
import com.pullit.domain.assignment.entity.Submission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentListResponse {
    
    private Long id;
    private String title;
    private String description;
    private Long teacherId;
    private LocalDateTime dueDate;
    private Assignment.AssignmentStatus status;
    private Integer maxScore;
    private Boolean allowLateSubmission;
    private LocalDateTime createdAt;
    
    // 학생용 추가 필드
    private Submission.SubmissionStatus submissionStatus;
    private LocalDateTime submittedAt;
    private Integer score;
    private Boolean isLate;
    
    // 선생님용 추가 필드
    private Integer totalStudents;
    private Integer submittedCount;
    private Integer gradedCount;
    
    public static AssignmentListResponse from(Assignment assignment) {
        return AssignmentListResponse.builder()
                .id(assignment.getId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .teacherId(assignment.getTeacherId())
                .dueDate(assignment.getDueDate())
                .status(assignment.getStatus())
                .maxScore(assignment.getMaxScore())
                .allowLateSubmission(assignment.getAllowLateSubmission())
                .createdAt(assignment.getCreatedAt())
                .totalStudents(assignment.getSubmissions().size())
                .submittedCount((int) assignment.getSubmissions().stream()
                        .filter(s -> s.getStatus() != Submission.SubmissionStatus.NOT_SUBMITTED)
                        .count())
                .gradedCount((int) assignment.getSubmissions().stream()
                        .filter(s -> s.getStatus() == Submission.SubmissionStatus.GRADED)
                        .count())
                .build();
    }
    
    public static AssignmentListResponse from(Assignment assignment, Submission submission) {
        AssignmentListResponseBuilder builder = AssignmentListResponse.builder()
                .id(assignment.getId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .teacherId(assignment.getTeacherId())
                .dueDate(assignment.getDueDate())
                .status(assignment.getStatus())
                .maxScore(assignment.getMaxScore())
                .allowLateSubmission(assignment.getAllowLateSubmission())
                .createdAt(assignment.getCreatedAt());
        
        if (submission != null) {
            builder.submissionStatus(submission.getStatus())
                    .submittedAt(submission.getSubmittedAt())
                    .score(submission.getScore())
                    .isLate(submission.getIsLate());
        } else {
            builder.submissionStatus(Submission.SubmissionStatus.NOT_SUBMITTED);
        }
        
        return builder.build();
    }
}
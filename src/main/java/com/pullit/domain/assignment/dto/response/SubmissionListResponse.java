package com.pullit.domain.assignment.dto.response;

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
public class SubmissionListResponse {
    
    private Long id;
    private Long assignmentId;
    private String assignmentTitle;
    private Long studentId;
    private String studentName;
    private Submission.SubmissionStatus status;
    private LocalDateTime submittedAt;
    private Integer score;
    private Boolean isLate;
    private Integer fileCount;
    
    public static SubmissionListResponse from(Submission submission) {
        return SubmissionListResponse.builder()
                .id(submission.getId())
                .assignmentId(submission.getAssignment().getId())
                .assignmentTitle(submission.getAssignment().getTitle())
                .studentId(submission.getStudentId())
                .studentName(submission.getStudentName())
                .status(submission.getStatus())
                .submittedAt(submission.getSubmittedAt())
                .score(submission.getScore())
                .isLate(submission.getIsLate())
                .fileCount(submission.getFiles().size())
                .build();
    }
}
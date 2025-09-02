package com.pullit.domain.assignment.dto.response;

import com.pullit.domain.assignment.entity.Submission;
import com.pullit.domain.assignment.entity.SubmissionFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionDetailResponse {
    
    private Long id;
    private Long assignmentId;
    private String assignmentTitle;
    private Long studentId;
    private String studentName;
    private Submission.SubmissionStatus status;
    private LocalDateTime submittedAt;
    private Integer score;
    private String feedback;
    private Boolean isLate;
    private List<FileInfo> files;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileInfo {
        private Long id;
        private String originalFileName;
        private String fileType;
        private Long fileSize;
        private LocalDateTime uploadedAt;
        
        public static FileInfo from(SubmissionFile file) {
            return FileInfo.builder()
                    .id(file.getId())
                    .originalFileName(file.getOriginalFileName())
                    .fileType(file.getFileType())
                    .fileSize(file.getFileSize())
                    .uploadedAt(file.getCreatedAt())
                    .build();
        }
    }
    
    public static SubmissionDetailResponse from(Submission submission) {
        return SubmissionDetailResponse.builder()
                .id(submission.getId())
                .assignmentId(submission.getAssignment().getId())
                .assignmentTitle(submission.getAssignment().getTitle())
                .studentId(submission.getStudentId())
                .studentName(submission.getStudentName())
                .status(submission.getStatus())
                .submittedAt(submission.getSubmittedAt())
                .score(submission.getScore())
                .feedback(submission.getFeedback())
                .isLate(submission.getIsLate())
                .files(submission.getFiles().stream()
                        .map(FileInfo::from)
                        .collect(Collectors.toList()))
                .createdAt(submission.getCreatedAt())
                .updatedAt(submission.getUpdatedAt())
                .build();
    }
}
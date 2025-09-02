package com.pullit.domain.assignment.dto.response;

import com.pullit.domain.assignment.entity.Assignment;
import com.pullit.domain.assignment.entity.AssignmentClass;
import com.pullit.domain.assignment.entity.AssignmentFile;
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
public class AssignmentDetailResponse {
    
    private Long id;
    private String title;
    private String description;
    private Long teacherId;
    private LocalDateTime dueDate;
    private Assignment.AssignmentStatus status;
    private Integer maxScore;
    private Boolean allowLateSubmission;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private List<FileInfo> files;
    private List<ClassInfo> classes;
    private SubmissionStats submissionStats;
    
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
        
        public static FileInfo from(AssignmentFile file) {
            return FileInfo.builder()
                    .id(file.getId())
                    .originalFileName(file.getOriginalFileName())
                    .fileType(file.getFileType())
                    .fileSize(file.getFileSize())
                    .uploadedAt(file.getCreatedAt())
                    .build();
        }
    }
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClassInfo {
        private Long id;
        private Long classId;
        private String className;
        
        public static ClassInfo from(AssignmentClass assignmentClass) {
            return ClassInfo.builder()
                    .id(assignmentClass.getId())
                    .classId(assignmentClass.getClassId())
                    .className(assignmentClass.getClassName())
                    .build();
        }
    }
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmissionStats {
        private Integer totalStudents;
        private Integer submittedCount;
        private Integer notSubmittedCount;
        private Integer gradedCount;
        private Integer lateSubmissionCount;
        
        public static SubmissionStats from(Assignment assignment) {
            int total = assignment.getSubmissions().size();
            int submitted = (int) assignment.getSubmissions().stream()
                    .filter(s -> s.getStatus() != com.pullit.domain.assignment.entity.Submission.SubmissionStatus.NOT_SUBMITTED)
                    .count();
            int graded = (int) assignment.getSubmissions().stream()
                    .filter(s -> s.getStatus() == com.pullit.domain.assignment.entity.Submission.SubmissionStatus.GRADED)
                    .count();
            int late = (int) assignment.getSubmissions().stream()
                    .filter(s -> Boolean.TRUE.equals(s.getIsLate()))
                    .count();
            
            return SubmissionStats.builder()
                    .totalStudents(total)
                    .submittedCount(submitted)
                    .notSubmittedCount(total - submitted)
                    .gradedCount(graded)
                    .lateSubmissionCount(late)
                    .build();
        }
    }
    
    public static AssignmentDetailResponse from(Assignment assignment) {
        return AssignmentDetailResponse.builder()
                .id(assignment.getId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .teacherId(assignment.getTeacherId())
                .dueDate(assignment.getDueDate())
                .status(assignment.getStatus())
                .maxScore(assignment.getMaxScore())
                .allowLateSubmission(assignment.getAllowLateSubmission())
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                .files(assignment.getFiles().stream()
                        .map(FileInfo::from)
                        .collect(Collectors.toList()))
                .classes(assignment.getAssignmentClasses().stream()
                        .map(ClassInfo::from)
                        .collect(Collectors.toList()))
                .submissionStats(SubmissionStats.from(assignment))
                .build();
    }
}
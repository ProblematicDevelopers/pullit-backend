package com.pullit.domain.assignment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionUploadRequest {
    
    @NotNull(message = "과제 ID는 필수입니다")
    private Long assignmentId;
    
    @NotNull(message = "학생 ID는 필수입니다")
    private Long studentId;
    
    private String comment;
}
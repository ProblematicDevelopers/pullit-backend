package com.pullit.domain.assignment.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentUpdateRequest {
    
    private String title;
    
    private String description;
    
    @Future(message = "마감일은 현재 시간 이후여야 합니다")
    private LocalDateTime dueDate;
    
    @Positive(message = "최대 점수는 양수여야 합니다")
    private Integer maxScore;
    
    private Boolean allowLateSubmission;
}
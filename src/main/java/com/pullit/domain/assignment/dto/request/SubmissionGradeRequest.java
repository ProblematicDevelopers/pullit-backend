package com.pullit.domain.assignment.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionGradeRequest {
    
    @Min(value = 0, message = "점수는 0점 이상이어야 합니다")
    private Integer score;
    
    private String feedback;
}
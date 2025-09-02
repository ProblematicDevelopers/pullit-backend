package com.pullit.domain.assignment.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentCreateRequest {
    
    @NotBlank(message = "과제 제목은 필수입니다")
    private String title;
    
    private String description;
    
    @NotNull(message = "선생님 ID는 필수입니다")
    private Long teacherId;
    
    @NotNull(message = "마감일은 필수입니다")
    @Future(message = "마감일은 현재 시간 이후여야 합니다")
    private LocalDateTime dueDate;
    
    @Positive(message = "최대 점수는 양수여야 합니다")
    private Integer maxScore;
    
    @Builder.Default
    private Boolean allowLateSubmission = false;
    
    private List<Long> classIds;
    
    private Map<Long, String> classNames;
}
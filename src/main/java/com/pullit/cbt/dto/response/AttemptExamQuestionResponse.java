package com.pullit.cbt.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AttemptExamQuestionResponse {
    private Long id;
    private Long questionId;
    private String userAnswer;
    private Boolean isCorrect;
    private Integer duration;
    private Integer points;
    private LocalDateTime answeredAt;
}

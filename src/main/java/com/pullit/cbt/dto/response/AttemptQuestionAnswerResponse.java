package com.pullit.cbt.dto.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AttemptQuestionAnswerResponse {
    private Long questionId;
    private Long itemId;
    private Integer itemOrder;
    private String userAnswer;
    private Boolean isCorrect;
    private Integer duration;
    private Integer points;
    private String answeredAt;
    private Boolean isAnswered;
}

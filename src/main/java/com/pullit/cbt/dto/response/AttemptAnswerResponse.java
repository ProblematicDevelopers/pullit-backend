package com.pullit.cbt.dto.response;

import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AttemptAnswerResponse {
    private Long attemptId;
    private Long examId;
    private String examName;
    private String status;
    private List<AttemptQuestionAnswerResponse> answers;
    private Integer totalQuestions;
    private Integer answeredQuestions;
    private Integer correctAnswers;
    private Integer totalScore;
    private Integer maxScore;
}

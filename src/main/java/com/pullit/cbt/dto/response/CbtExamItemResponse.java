package com.pullit.cbt.dto.response;

import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CbtExamItemResponse {
    private Long itemId;
    private Long subjectId;
    private Integer itemOrder;
    private Integer points;
    private String questionText;
    private String questionType;
    
    // ItemHtmlData 정보
    private String passage;
    private String passageHtml;
    private String question;
    private String questionHtml;
    private List<String> choices;
    private String answer;
    private String answerHtml;
    private String explainText;
    private String explainHtml;
}

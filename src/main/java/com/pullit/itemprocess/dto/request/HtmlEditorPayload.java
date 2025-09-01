package com.pullit.itemprocess.dto.request;

import lombok.*;

import java.util.List;

@Data
public class HtmlEditorPayload {
    // 에디터에서 넘어오는 “렌더용 HTML”
    private String passageHtml;
    private String questionHtml;
    private String choice1Html;
    private String choice2Html;
    private String choice3Html;
    private String choice4Html;
    private String choice5Html;
    private String answerHtml;
    private String explainHtml;

    // 선택: 원문 텍스트를 FE가 같이 줄 수도 있음(없으면 서버가 추출)
    private String passageText;
    private String questionText;
    private String answerText;
    private String explainText;
}

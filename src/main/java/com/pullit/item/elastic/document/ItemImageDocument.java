package com.pullit.item.elastic.document;

import co.elastic.clients.json.JsonpDeserializable;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonpDeserializable
public class ItemImageDocument {
    private Long itemId;
    private String passageUrl;
    private String questionUrl;
    private String answerUrl;
    private String explainUrl;
    private Long subjectId;
    private Long largeChapterId;
    private Long mediumChapterId;
    private Long smallChapterId;
    private Long topicChapterId;
    private Long difficultyCode;
}

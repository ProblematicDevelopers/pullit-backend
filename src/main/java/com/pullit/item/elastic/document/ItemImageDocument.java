package com.pullit.item.elastic.document;

import co.elastic.clients.json.JsonpDeserializable;
import co.elastic.clients.json.JsonpDeserializer;
import co.elastic.clients.json.JsonpSerializable;
import co.elastic.clients.json.ObjectBuilderDeserializer;
import co.elastic.clients.json.ObjectDeserializer;
import co.elastic.clients.util.ObjectBuilder;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.json.stream.JsonGenerator;
import co.elastic.clients.json.JsonpMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonpDeserializable
public class ItemImageDocument implements JsonpSerializable {

    @JsonProperty("item_id")
    private Long itemId;

    @JsonProperty("passage_id")
    private Long passageId;

    @JsonProperty("passage_url")
    private String passageUrl;

    @JsonProperty("question_url")
    private String questionUrl;

    @JsonProperty("answer_url")
    private String answerUrl;

    @JsonProperty("explain_url")
    private String explainUrl;

    @JsonProperty("difficulty_code")
    private Long difficultyCode;

    @JsonProperty("subject_id")
    private Long subjectId;

    @JsonProperty("large_chapter_id")
    private Long largeChapterId;

    @JsonProperty("medium_chapter_id")
    private Long mediumChapterId;

    @JsonProperty("small_chapter_id")
    private Long smallChapterId;

    @JsonProperty("topic_chapter_id")
    private Long topicChapterId;

    @Override
    public void serialize(JsonGenerator jsonGenerator, JsonpMapper jsonpMapper) {
        jsonGenerator.writeStartObject();
        if (itemId != null) { jsonGenerator.write("item_id", itemId); }
        if (passageId != null) { jsonGenerator.write("passage_id", passageId); }
        if (passageUrl != null) { jsonGenerator.write("passage_url", passageUrl); }
        if (questionUrl != null) { jsonGenerator.write("question_url", questionUrl); }
        if (answerUrl != null) { jsonGenerator.write("answer_url", answerUrl); }
        if (explainUrl != null) { jsonGenerator.write("explain_url", explainUrl); }
        if (difficultyCode != null) { jsonGenerator.write("difficulty_code", difficultyCode); }
        if (subjectId != null) { jsonGenerator.write("subject_id", subjectId); }
        if (largeChapterId != null) { jsonGenerator.write("large_chapter_id", largeChapterId); }
        if (mediumChapterId != null) { jsonGenerator.write("medium_chapter_id", mediumChapterId); }
        if (smallChapterId != null) { jsonGenerator.write("small_chapter_id", smallChapterId); }
        if (topicChapterId != null) { jsonGenerator.write("topic_chapter_id", topicChapterId); }
        jsonGenerator.writeEnd();
    }

    public static final JsonpDeserializer<ItemImageDocument> _DESERIALIZER =
            ObjectBuilderDeserializer.lazy(Builder::new, ItemImageDocument::setupItemImageDocumentDeserializer);

    protected static void setupItemImageDocumentDeserializer(ObjectDeserializer<ItemImageDocument.Builder> op) {
        op.add(Builder::itemId, JsonpDeserializer.longDeserializer(), "item_id");
        op.add(Builder::passageId, JsonpDeserializer.longDeserializer(), "passage_id");
        op.add(Builder::passageUrl, JsonpDeserializer.stringDeserializer(), "passage_url");
        op.add(Builder::questionUrl, JsonpDeserializer.stringDeserializer(), "question_url");
        op.add(Builder::answerUrl, JsonpDeserializer.stringDeserializer(), "answer_url");
        op.add(Builder::explainUrl, JsonpDeserializer.stringDeserializer(), "explain_url");
        op.add(Builder::difficultyCode, JsonpDeserializer.longDeserializer(), "difficulty_code");
        op.add(Builder::subjectId, JsonpDeserializer.longDeserializer(), "subject_id");
        op.add(Builder::largeChapterId, JsonpDeserializer.longDeserializer(), "large_chapter_id");
        op.add(Builder::mediumChapterId, JsonpDeserializer.longDeserializer(), "medium_chapter_id");
        op.add(Builder::smallChapterId, JsonpDeserializer.longDeserializer(), "small_chapter_id");
        op.add(Builder::topicChapterId, JsonpDeserializer.longDeserializer(), "topic_chapter_id");
    }

    public static class Builder implements ObjectBuilder<ItemImageDocument> {
        private Long itemId;
        private Long passageId;
        private String passageUrl;
        private String questionUrl;
        private String answerUrl;
        private String explainUrl;
        private Long difficultyCode;
        private Long subjectId;
        private Long largeChapterId;
        private Long mediumChapterId;
        private Long smallChapterId;
        private Long topicChapterId;

        public Builder itemId(Long itemId) {
            this.itemId = itemId;
            return this;
        }

        public Builder passageId(Long passageId) {
            this.passageId = passageId;
            return this;
        }

        public Builder passageUrl(String passageUrl) {
            this.passageUrl = passageUrl;
            return this;
        }
        public Builder questionUrl(String questionUrl) {
            this.questionUrl = questionUrl;
            return this;
        }
        public Builder answerUrl(String answerUrl) {
            this.answerUrl = answerUrl;
            return this;
        }
        public Builder explainUrl(String explainUrl) {
            this.explainUrl = explainUrl;
            return this;
        }
        public Builder difficultyCode(Long difficultyCode) {
            this.difficultyCode = difficultyCode;
            return this;
        }
        public Builder subjectId(Long subjectId) {
            this.subjectId = subjectId;
            return this;
        }
        public Builder largeChapterId(Long largeChapterId) {
            this.largeChapterId = largeChapterId;
            return this;
        }
        public Builder mediumChapterId(Long mediumChapterId) {
            this.mediumChapterId = mediumChapterId;
            return this;
        }
        public Builder smallChapterId(Long smallChapterId) {
            this.smallChapterId = smallChapterId;
            return this;
        }
        public Builder topicChapterId(Long topicChapterId) {
            this.topicChapterId = topicChapterId;
            return this;
        }

        public ItemImageDocument build() {
            return new ItemImageDocument(
                    itemId,
                    passageId,
                    passageUrl,
                    questionUrl,
                    answerUrl,
                    explainUrl,
                    difficultyCode,
                    subjectId,
                    largeChapterId,
                    mediumChapterId,
                    smallChapterId,
                    topicChapterId
            );
        }
    }
}

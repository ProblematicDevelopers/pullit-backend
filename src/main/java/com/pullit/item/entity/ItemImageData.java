package com.pullit.item.entity;

import com.pullit.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "item_image_data")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(exclude = "itemMetadata")
public class ItemImageData extends BaseTimeEntity {

    @Id
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "item_id")
    private ItemMetadata itemMetadata;

    @Column(name = "passage_url", length = 500)
    private String passageUrl;

    @Column(name = "question_url", length = 500)
    private String questionUrl;

    @Column(name = "answer_url", length = 500)
    private String answerUrl;

    @Column(name = "explain_url", length = 500)
    private String explainUrl;

    void setItemMetadata(ItemMetadata itemMetadata) {
        this.itemMetadata = itemMetadata;
        if (itemMetadata != null) {
            this.itemId = itemMetadata.getItemId();
        }
    }

    public boolean hasImages() {
        return hasQuestionImage() || hasPassageImage()
                || hasAnswerImage() || hasExplainImage();
    }

    public boolean hasQuestionImage() {
        return questionUrl != null && !questionUrl.trim().isEmpty();
    }

    public boolean hasPassageImage() {
        return passageUrl != null && !passageUrl.trim().isEmpty();
    }
    public boolean hasAnswerImage() {
        return answerUrl != null && !answerUrl.trim().isEmpty();
    }
    public boolean hasExplainImage() {
        return explainUrl != null && !explainUrl.trim().isEmpty();
    }

    public List<String> getAllImageUrls() {
        List<String> urls = new ArrayList<>();
        if (passageUrl != null) urls.add(passageUrl);
        if (questionUrl != null) urls.add(questionUrl);
        if (answerUrl != null) urls.add(answerUrl);
        if (explainUrl != null) urls.add(explainUrl);
        return urls;
    }

}

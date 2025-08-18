package com.pullit.item.entity;

import com.pullit.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "item_html_data")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(exclude = "itemMetadata")
public class ItemHtmlData extends BaseTimeEntity {

    @Id
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    // ItemMetadata와 1:1 관계, @MapsId로 PK 공유
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "item_id")
    private ItemMetadata itemMetadata;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String passage;

    @Lob
    @Column(name = "passage_html", columnDefinition = "TEXT")
    private String passageHtml;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String question;

    @Lob
    @Column(name = "question_html", columnDefinition = "TEXT")
    private String questionHtml;

    @Lob
    @Column(name = "choice1_html", columnDefinition = "TEXT")
    private String choice1Html;

    @Lob
    @Column(name = "choice2_html", columnDefinition = "TEXT")
    private String choice2Html;

    @Lob
    @Column(name = "choice3_html", columnDefinition = "TEXT")
    private String choice3Html;

    @Lob
    @Column(name = "choice4_html", columnDefinition = "TEXT")
    private String choice4Html;

    @Lob
    @Column(name = "choice5_html", columnDefinition = "TEXT")
    private String choice5Html;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String answer;

    @Lob
    @Column(name = "answer_html", columnDefinition = "TEXT")
    private String answerHtml;

    @Lob
    @Column(name = "explain_text", columnDefinition = "TEXT")
    private String explainText;

    @Lob
    @Column(name = "explain_html", columnDefinition = "TEXT")
    private String explainHtml;

    void setItemMetadata(ItemMetadata itemMetadata) {
        this.itemMetadata = itemMetadata;
        if (itemMetadata != null) {
            this.itemId = itemMetadata.getItemId();
        }
    }

    public List<String> getChoicesAsList() {
        List<String> choices = new ArrayList<>();
        if (choice1Html != null) choices.add(choice1Html);
        if (choice2Html != null) choices.add(choice2Html);
        if (choice3Html != null) choices.add(choice3Html);
        if (choice4Html != null) choices.add(choice4Html);
        if (choice5Html != null) choices.add(choice5Html);
        return choices;
    }

    public int getValidChoiceCount() {
        int count = 0;
        if (choice1Html != null && !choice1Html.trim().isEmpty()) count++;
        if (choice2Html != null && !choice2Html.trim().isEmpty()) count++;
        if (choice3Html != null && !choice3Html.trim().isEmpty()) count++;
        if (choice4Html != null && !choice4Html.trim().isEmpty()) count++;
        if (choice5Html != null && !choice5Html.trim().isEmpty()) count++;
        return count;
    }

    public boolean hasExplanation() {
        return (explainHtml != null && !explainHtml.trim().isEmpty())
                || (explainText != null && !explainText.trim().isEmpty());
    }
}
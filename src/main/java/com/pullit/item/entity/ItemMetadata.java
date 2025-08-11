package com.pullit.item.entity;

import com.pullit.common.entity.BaseTimeEntity;
import com.pullit.item.embedded.ChapterHierarchy;
import com.pullit.item.embedded.CodeNamePair;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "item_metadata")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(exclude = {"htmlData", "imageData"})
public class ItemMetadata extends BaseTimeEntity {
    @Id
    @Column(name = "item_id", nullable = false, length = 50)
    private Long itemId;

    @Column(name = "subject_id")
    private Long subjectId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "question_form_code")),
            @AttributeOverride(name = "name", column = @Column(name = "question_form_name"))
    })
    private CodeNamePair questionForm;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "difficulty_code")),
            @AttributeOverride(name = "name", column = @Column(name = "difficulty_name"))
    })
    private CodeNamePair difficulty;

    @Embedded
    private ChapterHierarchy chapterHierarchy;

    @Column(name="passage_id")
    private Long passageId;

    @Column(name="has_html_data")
    @Builder.Default
    private Boolean hasHtmlData = false;

    @Column(name = "has_image_data")
    @Builder.Default
    private Boolean hasImageData = false;

    @OneToOne(mappedBy = "itemMetadata", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private ItemHtmlData htmlData;

    @OneToOne(mappedBy = "itemMetadata", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private ItemImageData imageData;

    public void setHtmlData(ItemHtmlData htmlData) {
        if (this.htmlData != null) {
            this.htmlData.setItemMetadata(null);
        }
        this.htmlData = htmlData;
        this.hasHtmlData = (htmlData != null);
        if (htmlData != null && htmlData.getItemMetadata() != this) {
            htmlData.setItemMetadata(this);
        }
    }

    public void setImageData(ItemImageData imageData) {
        if (this.imageData != null) {
            this.imageData.setItemMetadata(null);
        }
        this.imageData = imageData;
        this.hasImageData = (imageData != null);
        if (imageData != null && imageData.getItemMetadata() != this) {
            imageData.setItemMetadata(this);
        }


    }

    public boolean isComplete() {
        return itemId != null && subjectId != null
                && questionForm != null && questionForm.isValid()
                && difficulty != null && difficulty.isValid();
    }

    public boolean hasPassage() {
        return passageId != null;
    }




}

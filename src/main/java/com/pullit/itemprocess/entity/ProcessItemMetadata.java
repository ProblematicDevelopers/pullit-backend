package com.pullit.itemprocess.entity;

import com.pullit.common.entity.BaseTimeEntity;
import com.pullit.item.embedded.ChapterHierarchy;
import com.pullit.item.embedded.CodeNamePair;
import com.pullit.item.entity.Subject;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "process_item_metadata")
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"htmlData", "imageData"})
public class ProcessItemMetadata extends BaseTimeEntity {
    @Id
    @Column(name = "item_id", nullable = false, length = 50)
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

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
    private ProcessItemHtmlData htmlData;

    @OneToOne(mappedBy = "itemMetadata", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private ProcessItemImageData imageData;

    public void setHtmlData(ProcessItemHtmlData htmlData) {
        if (this.htmlData != null) {
            this.htmlData.setItemMetadata(null);
        }
        this.htmlData = htmlData;
        this.hasHtmlData = (htmlData != null);
        if (htmlData != null && htmlData.getItemMetadata() != this) {
            htmlData.setItemMetadata(this);
        }
    }

    public void setImageData(ProcessItemImageData imageData) {
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
        return itemId != null && subject != null
                && questionForm != null && questionForm.isValid()
                && difficulty != null && difficulty.isValid();
    }

    public boolean hasPassage() {
        return passageId != null;
    }




}

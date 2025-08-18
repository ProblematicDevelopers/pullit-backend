package com.pullit.exam.entity;

import com.pullit.item.entity.ItemMetadata;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name="exam_items")
@Getter
@Setter
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(exclude= {"exam", "item"})
public class ExamItem {

    @EmbeddedId
    private ExamItemId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("examId")
    @JoinColumn(name = "exam_id")
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("itemId")
    @JoinColumn(name = "item_id")
    private ItemMetadata item;

    @Column(name = "item_no", nullable = false)
    private Integer itemNo;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class ExamItemId implements Serializable {
        @Column(name = "exam_id")
        private Long examId;

        @Column(name = "item_id")
        private Long itemId;
    }

    public void setExam(Exam exam) {
        this.exam = exam;
        if (exam != null && this.id != null) {
            this.id.setExamId(exam.getId());
        }
    }

    public void setItem(ItemMetadata item) {
        this.item = item;
        if (item != null && this.id != null) {
            this.id.setItemId(item.getItemId());
        }
    }

    public void updateItemNo(Integer newItemNo) {
        this.itemNo = newItemNo;
    }

    public boolean isFirstItem() {
        return itemNo != null && itemNo == 1;
    }

    public boolean isLastItem(int totalCount) {
        return itemNo != null && itemNo == totalCount;
    }

    public static ExamItem create(Exam exam, ItemMetadata item, Integer itemNo) {
        ExamItemId id = new ExamItemId(exam.getId(), item.getItemId());
        return ExamItem.builder()
                .id(id)
                .exam(exam)
                .item(item)
                .itemNo(itemNo)
                .build();
    }




}

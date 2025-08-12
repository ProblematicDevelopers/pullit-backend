package com.pullit.item.entity;

import com.pullit.subject.entity.Subject;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name="item_activity_mapping")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(exclude={"item","subject"})
public class ItemActivityMapping {

    @EmbeddedId
    private ItemActivityId id;

    @ManyToOne(fetch= FetchType.LAZY)
    @MapsId("itemId")
    @JoinColumn(name="item_id")
    private ItemMetadata item;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="subject_id")
    private Subject subject;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class ItemActivityId implements Serializable {
        @Column(name = "item_id")
        private Long itemId;

        @Column(name="activity_category_id")
        private Long activityCategoryId;

    }

}

package com.pullit.item.entity;


import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name="item_activity_mapping")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(exclude={"item","subject","evaluationDomain"})
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
    
    // activity_category_id는 실제로 evaluation_domain의 domain_id를 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_category_id", insertable = false, updatable = false)
    private EvaluationDomain evaluationDomain;

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
    
    // 비즈니스 메서드
    public String getEvaluationDomainName() {
        return evaluationDomain != null ? evaluationDomain.getDomainName() : null;
    }
    
    public boolean isEvaluationDomain(Long domainId) {
        return id != null && domainId.equals(id.getActivityCategoryId());
    }
    
    // 정적 팩토리 메서드
    public static ItemActivityMapping create(ItemMetadata item, EvaluationDomain domain, Subject subject) {
        ItemActivityId id = new ItemActivityId(item.getItemId(), domain.getDomainId());
        return ItemActivityMapping.builder()
                .id(id)
                .item(item)
                .evaluationDomain(domain)
                .subject(subject)
                .build();
    }
}

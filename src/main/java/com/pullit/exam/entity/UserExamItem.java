package com.pullit.exam.entity;

import com.pullit.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_exam_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
public class UserExamItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_exam_id", nullable = false)
    private UserExam userExam;

    @Column(name = "item_id", nullable = false)
    private Long itemId;  // item_metadata 테이블의 item_id만 저장

    @Column(name = "subject_id")
    private Long subjectId;  // subjects 테이블의 subject_id만 저장 (빠른 필터링용)

    @Column(name = "item_order", nullable = false)
    private Integer itemOrder;  // 문제 순서

    @Column(name = "points")
    private Integer points;  // 배점 (옵션)

    @Builder
    public UserExamItem(UserExam userExam, Long itemId, Long subjectId,
                        Integer itemOrder, Integer points) {
        this.userExam = userExam;
        this.itemId = itemId;
        this.subjectId = subjectId;
        this.itemOrder = itemOrder;
        this.points = points;
    }

}

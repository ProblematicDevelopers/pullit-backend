package com.pullit.exam.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "teacher_live_exam_items")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeacherLiveExamItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "live_exam_id", nullable = false)
    private TeacherLiveExam liveExam;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_exam_item_id", nullable = false)
    private UserExamItem userExamItem;
    
    @Column(name = "item_order", nullable = false)
    private Integer itemOrder;
    
    @Column(name = "points")
    @Builder.Default
    private Integer points = 5;
    
    // 편의 메서드
    public String getQuestionText() {
        // TODO: item_metadata 테이블에서 가져와야 함
        return null;
    }
    
    public String getQuestionHtml() {
        // TODO: item_metadata 테이블에서 가져와야 함
        return null;
    }
    
    public String getCorrectAnswer() {
        // TODO: item_metadata 테이블에서 가져와야 함
        return null;
    }
}
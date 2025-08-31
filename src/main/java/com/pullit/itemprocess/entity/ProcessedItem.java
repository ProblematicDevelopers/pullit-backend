package com.pullit.itemprocess.entity;

import com.pullit.common.entity.BaseTimeEntity;
import com.pullit.itemprocess.enums.ItemType;
import com.pullit.itemprocess.enums.DifficultyLevel;
import com.pullit.filehistory.entity.OcrHistory;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "processed_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(exclude = {"ocrHistories"})
public class ProcessedItem extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ItemType type;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 10)
    private DifficultyLevel difficulty;
    
    @Column(name = "answer", nullable = false, columnDefinition = "TEXT")
    private String answer;
    
    @Column(name = "score", nullable = true)
    private Integer score;
    
    @Column(name = "major_chapter_id")
    private Long majorChapterId;
    
    @Column(name = "middle_chapter_id")
    private Long middleChapterId;
    
    @Column(name = "minor_chapter_id")
    private Long minorChapterId;

    @Column(name = "topic_chapter_id")
    private Long topicChapterId;
    
    @Column(name = "solution", columnDefinition = "TEXT")
    private String solution;
    
    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;
    
    // 기존 구조를 따라 passageId로 지문 그룹 관리
    @Column(name = "passage_id")
    private Long passageId;
    
    // OCR History와의 연관관계
    @OneToMany(mappedBy = "processedItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OcrHistory> ocrHistories = new ArrayList<>();
    
    // 연관관계 편의 메서드
    public void addOcrHistory(OcrHistory ocrHistory) {
        this.ocrHistories.add(ocrHistory);
        ocrHistory.setProcessedItem(this);
    }
    
    public void removeOcrHistory(OcrHistory ocrHistory) {
        this.ocrHistories.remove(ocrHistory);
        ocrHistory.setProcessedItem(null);
    }
    
    // 비즈니스 로직
    public boolean hasPassage() {
        return this.passageId != null;
    }
    
    public boolean isComplete() {
        return type != null && difficulty != null && answer != null && 
               !answer.trim().isEmpty()  && majorChapterId != null;
    }
}
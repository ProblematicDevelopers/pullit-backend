package com.pullit.exam.entity;

import com.pullit.common.entity.FullAuditEntity;
import com.pullit.exam.enums.ExamVisibility;
import com.pullit.item.embedded.CodeNamePair;
import com.pullit.item.entity.Subject;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="exams")
@Getter
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(exclude={"subject","examItems"})
public class Exam extends FullAuditEntity {

    @Id
    @Column(name="exam_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="exam_name", length=500, nullable = false)
    private String examName;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="subject_id")
    private Subject subject;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "large_chapter_id")),
            @AttributeOverride(name = "name", column = @Column(name = "large_chapter_name", length = 200))
    })
    private CodeNamePair largeChapter;

    @Column(name = "item_count")
    @Builder.Default
    private Integer itemCount = 0;

    @Column(name="preview_url", length=500)
    private String previewUrl;

    @Column(name="file_url", length=500)
    private String fileUrl;

    @Column(name="is_public")
    @Builder.Default
    private Boolean isPublic = false;

    @Enumerated(EnumType.STRING)
    @Column(name="visibility")
    @Builder.Default
    private ExamVisibility visibility = ExamVisibility.SCHOOL;

    @OneToMany(mappedBy = "exam", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExamItem> examItems = new ArrayList<>();

    public void updateItemCount(){
        this.itemCount = examItems != null? examItems.size() :0;
    }

    public void addExamItem(ExamItem examItem){
        if(examItems==null){
            examItems=new ArrayList<>();
        }
        examItems.add(examItem);
        if(examItem.getExam() != this){
            examItem.setExam(this);
        }
        updateItemCount();
    }

    public void removeExamItem(ExamItem examItem) {
        if (examItems != null) {
            examItems.remove(examItem);
            examItem.setExam(null);
            updateItemCount();
        }
    }

    public boolean isAccessibleBy(String userId, Long schoolId) {
        // PUBLIC: 모든 사용자 접근 가능
        if (visibility == ExamVisibility.PUBLIC) {
            return true;
        }

        // PRIVATE: 생성자만 접근 가능
        if (visibility == ExamVisibility.PRIVATE) {
            return isOwnedBy(userId);
        }

        //TODO : SCHOOL: 같은 학교 사용자 접근 가능 (추후 구현)
        return false;
    }

    public void publish() {
        this.isPublic = true;
        this.visibility = ExamVisibility.PUBLIC;
    }

    public void makePrivate() {
        this.isPublic = false;
        this.visibility = ExamVisibility.PRIVATE;
    }

    public void shareWithSchool() {
        this.isPublic = false;
        this.visibility = ExamVisibility.SCHOOL;
    }

    public boolean hasLargeChapter() {
        return largeChapter != null && largeChapter.isValid();
    }

    public String getChapterDisplayName() {
        return largeChapter != null ? largeChapter.getDisplayName() : "";
    }
    public boolean isOwnedBy(String userId) {
        return getCreatedBy() != null && getCreatedBy().equals(userId);
    }



}

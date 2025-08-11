package com.pullit.chapter.entity;

import com.pullit.common.embedded.StringCodeNamePair;
import com.pullit.item.embedded.CodeNamePair;
import com.pullit.item.entity.ItemMetadata;
import com.pullit.subject.entity.Subject;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chapters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(exclude = {"subject", "items"})
public class Chapter {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "curriculum_code", length = 10)),
            @AttributeOverride(name = "name", column = @Column(name = "curriculum_name", length = 100))
    })
    private StringCodeNamePair curriculum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(name = "subject_name", length = 100)
    private String subjectName;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "large_chapter_id")),
            @AttributeOverride(name = "name", column = @Column(name = "large_chapter_name", length = 200))
    })
    private CodeNamePair largeChapter;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "medium_chapter_id")),
            @AttributeOverride(name = "name", column = @Column(name = "medium_chapter_name", length = 200))
    })
    private CodeNamePair mediumChapter;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "small_chapter_id")),
            @AttributeOverride(name = "name", column = @Column(name = "small_chapter_name", length = 200))
    })
    private CodeNamePair smallChapter;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "topic_chapter_id")),
            @AttributeOverride(name = "name", column = @Column(name = "topic_chapter_name", length = 200))
    })
    private CodeNamePair topicChapter;

    public String getChapterPath() {
        StringBuilder path = new StringBuilder();

        if (largeChapter != null && largeChapter.getName() != null) {
            path.append(largeChapter.getName());
        }
        if (mediumChapter != null && mediumChapter.getName() != null) {
            if (path.length() > 0) path.append(" > ");
            path.append(mediumChapter.getName());
        }
        if (smallChapter != null && smallChapter.getName() != null) {
            if (path.length() > 0) path.append(" > ");
            path.append(smallChapter.getName());
        }
        if (topicChapter != null && topicChapter.getName() != null) {
            if (path.length() > 0) path.append(" > ");
            path.append(topicChapter.getName());
        }

        return path.toString();
    }

    public int getChapterDepth() {
        if (topicChapter != null && topicChapter.getCode() != null) return 4;
        if (smallChapter != null && smallChapter.getCode() != null) return 3;
        if (mediumChapter != null && mediumChapter.getCode() != null) return 2;
        if (largeChapter != null && largeChapter.getCode() != null) return 1;
        return 0;
    }

    public boolean hasLargeChapter() {
        return largeChapter != null && largeChapter.getCode() != null;
    }

    public boolean hasMediumChapter() {
        return mediumChapter != null && mediumChapter.getCode() != null;
    }

    public boolean hasSmallChapter() {
        return smallChapter != null && smallChapter.getCode() != null;
    }

    public boolean hasTopicChapter() {
        return topicChapter != null && topicChapter.getCode() != null;
    }

    public boolean matchesHierarchy(Long largeId, Long mediumId, Long smallId, Long topicId) {
        boolean matches = true;

        if (largeId != null) {
            matches = matches && largeChapter != null && largeChapter.getCode().equals(largeId);
        }
        if (mediumId != null) {
            matches = matches && mediumChapter != null && mediumChapter.getCode().equals(mediumId);
        }
        if (smallId != null) {
            matches = matches && smallChapter != null && smallChapter.getCode().equals(smallId);
        }
        if (topicId != null) {
            matches = matches && topicChapter != null && topicChapter.getCode().equals(topicId);
        }

        return matches;
    }

}

package com.pullit.item.embedded;

import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChapterHierarchy {

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "large_chapter_id")),
            @AttributeOverride(name = "name", column = @Column(name = "large_chapter_name"))
    })
    private CodeNamePair largeChapter;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "medium_chapter_id")),
            @AttributeOverride(name = "name", column = @Column(name = "medium_chapter_name"))
    })
    private CodeNamePair mediumChapter;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "small_chapter_id")),
            @AttributeOverride(name = "name", column = @Column(name = "small_chapter_name"))
    })
    private CodeNamePair smallChapter;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "topic_chapter_id")),
            @AttributeOverride(name = "name", column = @Column(name = "topic_chapter_name"))
    })
    private CodeNamePair topicChapter;

    public String getFullPath() {
        StringBuilder path = new StringBuilder();

        if (largeChapter != null && largeChapter.isValid()) {
            path.append(largeChapter.getDisplayName());
        }
        if (mediumChapter != null && mediumChapter.isValid()) {
            if (path.length() > 0) path.append(" > ");
            path.append(mediumChapter.getDisplayName());
        }
        if (smallChapter != null && smallChapter.isValid()) {
            if (path.length() > 0) path.append(" > ");
            path.append(smallChapter.getDisplayName());
        }
        if (topicChapter != null && topicChapter.isValid()) {
            if (path.length() > 0) path.append(" > ");
            path.append(topicChapter.getDisplayName());
        }

        return path.toString();
    }

}



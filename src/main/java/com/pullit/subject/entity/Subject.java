package com.pullit.subject.entity;

import com.pullit.chapter.entity.Chapter;
import com.pullit.common.embedded.StringCodeNamePair;
import com.pullit.item.entity.ItemMetadata;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "subjects")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(exclude = {"chapters", "items"})
public class Subject {
    @Id
    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "subject_name", length = 100)
    private String subjectName;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "curriculum_code", length = 10)),
            @AttributeOverride(name = "name", column = @Column(name = "curriculum_name", length = 100))
    })
    private StringCodeNamePair curriculum;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "school_level_code", length = 10)),
            @AttributeOverride(name = "name", column = @Column(name = "school_level_name", length = 50))
    })
    private StringCodeNamePair schoolLevel;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "grade_code", length = 10)),
            @AttributeOverride(name = "name", column = @Column(name = "grade_name", length = 20))
    })
    private StringCodeNamePair grade;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "term_code", length = 10)),
            @AttributeOverride(name = "name", column = @Column(name = "term_name", length = 20))
    })
    private StringCodeNamePair term;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "area_code", length = 10)),
            @AttributeOverride(name = "name", column = @Column(name = "area_name", length = 50))
    })
    private StringCodeNamePair area;

    @OneToMany(mappedBy = "subject", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Chapter> chapters = new ArrayList<>();

    @OneToMany(mappedBy = "subject", fetch = FetchType.LAZY)
    @Builder.Default
    private List<ItemMetadata> items = new ArrayList<>();

    public String getFullGradeName() {
        StringBuilder name = new StringBuilder();
        if (schoolLevel != null) name.append(schoolLevel.getName());
        if (grade != null) name.append(" ").append(grade.getName());
        if (term != null) name.append(" ").append(term.getName());
        return name.toString().trim();
    }

    public boolean isHighSchool() {
        return schoolLevel != null &&
                (schoolLevel.hasCode("HIGH") || "고등학교".equals(schoolLevel.getName()));
    }

    public boolean isMiddleSchool() {
        return schoolLevel != null &&
                (schoolLevel.hasCode("MIDDLE") || "중학교".equals(schoolLevel.getName()));
    }

    public boolean isElementarySchool() {
        return schoolLevel != null &&
                (schoolLevel.hasCode("ELEMENTARY") || "초등학교".equals(schoolLevel.getName()));
    }

    public int getChapterCount() {
        return chapters != null ? chapters.size() : 0;
    }

    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

}

package com.pullit.classes.entity;

import com.pullit.common.entity.BaseTimeEntity;
import com.pullit.common.embedded.StringCodeNamePair;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "classes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Classes extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_id")
    private Long classId;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "class_name", length = 100)
    private String className;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "class_grade_code", length = 10)),
            @AttributeOverride(name = "name", column = @Column(name = "class_grade_name", length = 20))
    })
    private StringCodeNamePair classGrade;  // 07(1학년), 08(2학년), 09(3학년)

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "class_subject_code", length = 10)),
            @AttributeOverride(name = "name", column = @Column(name = "class_subject_name", length = 50))
    })
    private StringCodeNamePair classSubject;  // MA(수학), KO(국어), EN(영어), SC(과학), SO(사회)

    @OneToMany(mappedBy = "classes", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ClassTeacher> classTeachers = new ArrayList<>();
}

package com.pullit.classes.entity;

import com.pullit.common.entity.BaseTimeEntity;
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

    @Column(name = "class_grade")
    private Long classGrade;

    @Column(name = "class_subject", length = 100)
    private String classSubject;

    @OneToMany(mappedBy = "classes", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ClassTeacher> classTeachers = new ArrayList<>();
}

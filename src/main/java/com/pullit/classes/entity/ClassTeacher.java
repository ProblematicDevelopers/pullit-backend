package com.pullit.classes.entity;

import com.pullit.common.entity.BaseTimeEntity;
import com.pullit.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "class_teachers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassTeacher extends BaseTimeEntity {

    @EmbeddedId
    private ClassTeacherId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("classId")
    @JoinColumn(name = "class_id")
    private Classes classes;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("teacherId")
    @JoinColumn(name = "teacher_id")
    private User teacher;
}

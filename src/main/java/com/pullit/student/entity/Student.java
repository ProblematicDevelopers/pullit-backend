package com.pullit.student.entity;

import com.pullit.common.entity.BaseTimeEntity;
import com.pullit.user.entity.User;
import com.pullit.classes.entity.School;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="students")
@AllArgsConstructor
@Builder
@Getter
@Setter
@NoArgsConstructor

public class Student extends BaseTimeEntity {

    @Id
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name="user_id")
    private User user;

    @Column(name="class_group_id")
    private Long classGroupID;

    @Column(name = "student_no")
    private Long studentNo;

    @Column
    private Long grade;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="school_id")
    private School school;


}

package com.pullit.student.entity;

import com.pullit.common.entity.BaseTimeEntity;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id",nullable = false)
    private Long userId;

    @Column(name="class_group_id",nullable = false)
    private Long classGroupID;

    @Column(name = "student_no", nullable = false)
    private Long studentNo;

    @Column(nullable = false)
    private Long grade;


}

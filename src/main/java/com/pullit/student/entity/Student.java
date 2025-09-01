package com.pullit.student.entity;

import com.pullit.common.embedded.StringCodeNamePair;
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
    @Column(name="user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", insertable = false, updatable = false)
    private User user;

    @Column(name="class_group_id")
    private Long classGroupID;

    @Column(name = "student_no")
    private Long studentNo;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "grade_code", length = 10)),
            @AttributeOverride(name = "name", column = @Column(name = "grade_name", length = 50))
    })
    private StringCodeNamePair grade;  // 학년 정보 (코드/이름)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="school_id")
    private School school;

    public String getGradeDisplayName() {
        return grade != null ? grade.getDisplayName() : "";
    }

}

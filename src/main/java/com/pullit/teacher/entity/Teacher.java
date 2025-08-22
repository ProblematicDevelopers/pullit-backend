package com.pullit.teacher.entity;

import com.pullit.common.embedded.StringCodeNamePair;
import com.pullit.common.entity.BaseTimeEntity;
import com.pullit.user.entity.User;
import jakarta.persistence.*;

@Entity
@Table(name="teachers")
public class Teacher extends BaseTimeEntity {

    @Id
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name="user_id")
    private User user;

    @Column(name="school_name")
    private Long schoolId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "area_code", length = 10)),
            @AttributeOverride(name = "name", column = @Column(name = "area_name", length = 50))
    })
    private StringCodeNamePair area;


    public String getAreaDisplayName() {
        return area != null ? area.getDisplayName() : "";
    }

}

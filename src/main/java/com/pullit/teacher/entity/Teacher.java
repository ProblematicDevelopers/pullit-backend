package com.pullit.teacher.entity;

import com.pullit.common.embedded.StringCodeNamePair;
import com.pullit.common.entity.BaseTimeEntity;
import com.pullit.user.entity.User;
import com.pullit.classes.entity.School;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="teachers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Teacher extends BaseTimeEntity {

    @Id
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name="user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="school_id")
    private School school;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "area_code", length = 10)),
            @AttributeOverride(name = "name", column = @Column(name = "area_name", length = 50))
    })
    private StringCodeNamePair area;  // 과목 정보 (코드/이름)


    public String getAreaDisplayName() {
        return area != null ? area.getDisplayName() : "";
    }

}

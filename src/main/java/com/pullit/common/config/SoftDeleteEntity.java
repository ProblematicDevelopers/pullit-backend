package com.pullit.common.config;

import com.pullit.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@SQLDelete(sql= "UPDATE {tableName} SET deleted_at = NOW() WHERE id= ?")
public class SoftDeleteEntity extends BaseEntity {

    @Setter
    @Column(name ="deleted_at")
    private LocalDateTime deletedAt;

    @Column(name="deleted_by")
    private String deletedBy;

    public void restore(){
        this.deletedAt= null;
        this.deletedBy = null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}

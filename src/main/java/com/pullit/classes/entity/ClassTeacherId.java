package com.pullit.classes.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassTeacherId implements Serializable {

    private Long ctId;
    private Long classId;
    private Long teacherId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassTeacherId that = (ClassTeacherId) o;
        return Objects.equals(ctId, that.ctId) && 
               Objects.equals(classId, that.classId) && 
               Objects.equals(teacherId, that.teacherId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ctId, classId, teacherId);
    }
}

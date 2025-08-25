package com.pullit.classes.repository;

import com.pullit.classes.entity.ClassTeacher;
import com.pullit.classes.entity.ClassTeacherId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassTeacherRepository extends JpaRepository<ClassTeacher, ClassTeacherId> {
    
    List<ClassTeacher> findByClasses_ClassId(Long classId);
    
    List<ClassTeacher> findByTeacher_Id(Long teacherId);
}

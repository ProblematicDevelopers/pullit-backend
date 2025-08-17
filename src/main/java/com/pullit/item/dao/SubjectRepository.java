package com.pullit.item.dao;

import com.pullit.item.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Integer>{
    
    @Query("SELECT s FROM Subject s WHERE " +
           "(:gradeCode IS NULL OR s.grade.code = :gradeCode) AND " +
           "(:areaCode IS NULL OR s.area.code = :areaCode)")
    List<Subject> findByGradeCodeAndAreaCode(@Param("gradeCode") String gradeCode, 
                                              @Param("areaCode") String areaCode);
}

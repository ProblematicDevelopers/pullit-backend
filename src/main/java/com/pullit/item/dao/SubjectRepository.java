package com.pullit.item.dao;

import com.pullit.item.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long>{
    
    @Query("SELECT s FROM Subject s WHERE " +
           "(:gradeCode IS NULL OR s.grade.code = :gradeCode) AND " +
           "(:areaCode IS NULL OR s.area.code = :areaCode)")
    List<Subject> findByGradeCodeAndAreaCode(@Param("gradeCode") String gradeCode, 
                                              @Param("areaCode") String areaCode);

    // 과목별 문항 수 집계 - 전체
    @Query("SELECT s.subjectId AS subjectId, COUNT(i) AS itemCount " +
           "FROM Subject s LEFT JOIN s.items i " +
           "GROUP BY s.subjectId")
    List<SubjectRepository.SubjectItemCountProjection> countItemsAll();

    // 과목별 문항 수 집계 - 학년/과목 코드로 필터
    @Query("SELECT s.subjectId AS subjectId, COUNT(i) AS itemCount " +
           "FROM Subject s LEFT JOIN s.items i " +
           "WHERE (:gradeCode IS NULL OR s.grade.code = :gradeCode) " +
           "AND (:areaCode IS NULL OR s.area.code = :areaCode) " +
           "GROUP BY s.subjectId")
    List<SubjectRepository.SubjectItemCountProjection> countItemsByGradeAndArea(@Param("gradeCode") String gradeCode,
                                                                                @Param("areaCode") String areaCode);

    interface SubjectItemCountProjection {
        Long getSubjectId();
        Long getItemCount();
    }
}

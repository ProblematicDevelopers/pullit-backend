package com.pullit.classes.repository;

import com.pullit.classes.entity.Classes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRepository extends JpaRepository<Classes, Long> {

    // 교사 ID로 클래스 목록 조회
    List<Classes> findByTeacherId(Long teacherId);
    
    // 교사 ID로 첫 번째 클래스 조회 (교사가 하나의 클래스만 담당한다고 가정)
    Optional<Classes> findFirstByTeacherId(Long teacherId);

    // 교사 ID와 클래스 ID로 클래스 조회
    Optional<Classes> findByTeacherIdAndClassId(Long teacherId, Long classId);

    // 클래스 이름으로 검색
    List<Classes> findByClassNameContainingIgnoreCase(String className);

    // 과목으로 클래스 목록 조회
    List<Classes> findByClassSubject(String classSubject);

    // 학년으로 클래스 목록 조회
    List<Classes> findByClassGrade(Long classGrade);

    // 특정 학생이 속한 클래스 목록 조회 (임시로 빈 리스트 반환)
    @Query("SELECT c FROM Classes c WHERE c.teacherId = :studentId")
    List<Classes> findClassesByStudentId(@Param("studentId") Long studentId);

    // 클래스에 속한 교사 수 조회
    @Query("SELECT COUNT(ct) FROM ClassTeacher ct WHERE ct.classes.classId = :classId")
    Long countTeachersByClassId(@Param("classId") Long classId);
}

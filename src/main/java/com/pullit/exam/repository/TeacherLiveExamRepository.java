package com.pullit.exam.repository;

import com.pullit.exam.entity.TeacherLiveExam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherLiveExamRepository extends JpaRepository<TeacherLiveExam, Long> {
    
    // 특정 클래스의 모든 실시간 시험 조회
    @Query("SELECT tle FROM TeacherLiveExam tle " +
           "WHERE tle.examClass.id = :classId " +
           "AND tle.examStatus != 'CANCELLED' " +
           "ORDER BY tle.createdDate DESC")
    List<TeacherLiveExam> findByClassId(@Param("classId") Long classId);
    
    // 특정 클래스의 활성 시험 조회
    @Query("SELECT tle FROM TeacherLiveExam tle " +
           "WHERE tle.examClass.id = :classId " +
           "AND tle.examStatus IN ('CREATED', 'STARTED') " +
           "ORDER BY tle.scheduledDate ASC, tle.scheduledTime ASC")
    List<TeacherLiveExam> findActiveExamsByClassId(@Param("classId") Long classId);
    
    // 현재 진행 중인 시험 조회
    @Query("SELECT tle FROM TeacherLiveExam tle " +
           "WHERE tle.examClass.id = :classId " +
           "AND tle.examStatus = 'STARTED'")
    Optional<TeacherLiveExam> findCurrentExamByClassId(@Param("classId") Long classId);
    
    // 오늘 예정된 시험 조회
    @Query("SELECT tle FROM TeacherLiveExam tle " +
           "WHERE tle.examClass.id = :classId " +
           "AND tle.scheduledDate = :today " +
           "AND tle.examStatus = 'CREATED' " +
           "ORDER BY tle.scheduledTime ASC")
    List<TeacherLiveExam> findTodaysExamsByClassId(
            @Param("classId") Long classId, 
            @Param("today") LocalDate today);
    
    // 선생님이 만든 모든 시험 조회
    @Query("SELECT tle FROM TeacherLiveExam tle " +
           "WHERE tle.teacher.id = :teacherId " +
           "ORDER BY tle.createdDate DESC")
    List<TeacherLiveExam> findByTeacherId(@Param("teacherId") Long teacherId);
    
    // 시험 ID와 클래스 ID로 조회 (권한 체크용)
    @Query("SELECT tle FROM TeacherLiveExam tle " +
           "WHERE tle.id = :examId " +
           "AND tle.examClass.id = :classId")
    Optional<TeacherLiveExam> findByIdAndClassId(
            @Param("examId") Long examId, 
            @Param("classId") Long classId);

    // 원본 UserExam(시험지) 기준으로 해당 클래스를 대상으로 한 실시간 시험 존재 여부 (예정/진행)
    @Query("SELECT COUNT(DISTINCT tle) FROM TeacherLiveExam tle " +
           "JOIN tle.examItems tlei " +
           "JOIN tlei.userExamItem uei " +
           "WHERE uei.userExam.id = :sourceExamId " +
           "AND tle.examClass.id = :classId " +
           "AND tle.examStatus IN ('CREATED','STARTED')")
    long countActiveBySourceExamAndClass(@Param("sourceExamId") Long sourceExamId, @Param("classId") Long classId);

    // 원본 UserExam(시험지) 기준으로 최근 생성된 실시간 시험 조회
    @Query("SELECT DISTINCT tle FROM TeacherLiveExam tle " +
           "JOIN tle.examItems tlei " +
           "JOIN tlei.userExamItem uei " +
           "WHERE uei.userExam.id = :sourceExamId " +
           "AND tle.examClass.id = :classId " +
           "AND tle.examStatus IN ('CREATED','STARTED') " +
           "ORDER BY tle.createdDate DESC")
    List<TeacherLiveExam> findRecentBySourceExamAndClass(@Param("sourceExamId") Long sourceExamId, @Param("classId") Long classId);
}

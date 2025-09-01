package com.pullit.cbt.repository;

import com.pullit.cbt.entity.AttemptExam;
import com.pullit.exam.entity.UserExam;
import com.pullit.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttemptExamRepository extends JpaRepository<AttemptExam, Long> {
    AttemptExam findByUserAndExamAndStatus(User user, UserExam exam, AttemptExam.AttemptStatus status);
    
    // 대시보드용 추가 메서드
    
    /**
     * 선생님의 시험에 대한 최근 응시 기록 조회
     */
    @Query("SELECT ae FROM AttemptExam ae " +
           "JOIN ae.exam e " +
           "WHERE e.createdBy = :teacherId " +
           "ORDER BY ae.createdDate DESC")
    List<AttemptExam> findRecentAttemptsByTeacher(@Param("teacherId") Long teacherId, Pageable pageable);
    
    /**
     * 최근 채점 완료된 시험 조회
     */
    @Query("SELECT ae FROM AttemptExam ae " +
           "JOIN ae.exam e " +
           "WHERE e.createdBy = :teacherId " +
           "AND ae.status = 'DONE' " +
           "ORDER BY ae.updatedDate DESC")
    List<AttemptExam> findRecentlyGraded(@Param("teacherId") Long teacherId, Pageable pageable);
    
    /**
     * 시험별 응시자 수 조회
     */
    @Query("SELECT COUNT(ae) FROM AttemptExam ae WHERE ae.exam.id = :examId")
    Integer countByExamId(@Param("examId") Long examId);
    
    /**
     * 선생님 시험의 평균 점수 조회
     * TODO: AttemptExam에 score 필드 추가 후 구현
     */
    @Query("SELECT 0.0 FROM AttemptExam ae WHERE ae.id = -1")
    Double getAverageScoreByTeacher(@Param("teacherId") Long teacherId);
}

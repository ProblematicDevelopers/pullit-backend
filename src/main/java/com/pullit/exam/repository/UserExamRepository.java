package com.pullit.exam.repository;

import com.pullit.exam.entity.UserExam;
import com.pullit.exam.enums.ExamVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * UserExam(사용자 생성 시험) Repository
 * - 사용자가 직접 만든 시험 데이터 관리
 * - Soft Delete 지원 (deletedDate가 null인 것만 조회)
 * - 여러 과목 문제를 포함할 수 있음 (subjectId 필드 없음)
 */
@Repository
public interface UserExamRepository extends JpaRepository<UserExam, Long>, QuerydslPredicateExecutor<UserExam> {

    // ===== Soft Delete 고려 기본 메서드 =====
    // UserExam 엔티티에 @SQLRestriction("deleted_date IS NULL")이 있다면 자동 처리
    // 없다면 아래 메서드들 사용

    /**
     * 삭제되지 않은 모든 시험 조회
     */
    @Query("SELECT ue FROM UserExam ue WHERE ue.deletedDate IS NULL")
    List<UserExam> findAllActive();

    /**
     * 시험 이름으로 검색 (삭제된 것 제외)
     */
    List<UserExam> findByExamNameContainingIgnoreCaseAndDeletedDateIsNull(String keyword);

    /**
     * 생성자 ID로 검색 (삭제된 것 제요)
     * createdBy는 Long 타입 userId
     */
    @Query("SELECT ue FROM UserExam ue " +
            "WHERE ue.createdBy = :userId " +
            "AND ue.deletedDate IS NULL " +
            "ORDER BY ue.createdDate DESC")
    List<UserExam> findByCreatedByActive(@Param("userId") Long userId);

    /**
     * 특정 과목 문제를 포함한 시험 검색
     * - UserExamItem을 통해 과목 정보 확인
     * - UserExam 자체는 여러 과목 문제 포함 가능
     */
    @Query("SELECT DISTINCT ue FROM UserExam ue " +
            "JOIN ue.examItems uei " +
            "WHERE uei.subjectId = :subjectId " +
            "AND ue.deletedDate IS NULL")
    List<UserExam> findByItemSubjectId(@Param("subjectId") Long subjectId);

    /**
     * 학년 코드로 검색 (삭제된 것 제외)
     */
    List<UserExam> findByGradeCodeAndDeletedDateIsNull(String gradeCode);

    /**
     * 학기 코드로 검색 (삭제된 것 제외)
     */
    List<UserExam> findByTermCodeAndDeletedDateIsNull(String termCode);

    /**
     * 학년과 학기로 검색 (삭제된 것 제외)
     */
    List<UserExam> findByGradeCodeAndTermCodeAndDeletedDateIsNull(String gradeCode, String termCode);

    /**
     * 공개 여부로 검색 (삭제된 것 제외)
     */
    List<UserExam> findByVisibilityAndDeletedDateIsNull(ExamVisibility visibility);

    // ===== 복합 조건 검색 =====

    /**
     * 학년, 학기, 공개여부로 검색 (삭제된 것 제외)
     */
    List<UserExam> findByGradeCodeAndTermCodeAndVisibilityAndDeletedDateIsNull(
            String gradeCode,
            String termCode,
            ExamVisibility visibility
    );

    /**
     * 특정 과목 문제를 포함하고 학년, 학기가 일치하는 시험 검색
     */
    @Query("SELECT DISTINCT ue FROM UserExam ue " +
            "JOIN ue.examItems uei " +
            "WHERE uei.subjectId = :subjectId " +
            "AND ue.gradeCode = :gradeCode " +
            "AND ue.termCode = :termCode " +
            "AND ue.deletedDate IS NULL")
    List<UserExam> findByItemSubjectIdAndGradeCodeAndTermCode(
            @Param("subjectId") Long subjectId,
            @Param("gradeCode") String gradeCode,
            @Param("termCode") String termCode
    );

    // ===== 페이징 지원 메서드 =====

    /**
     * 사용자별 시험 목록 (페이징, 삭제된 것 제외)
     * createdBy는 Long 타입 userId
     */
    Page<UserExam> findByCreatedByAndDeletedDateIsNull(Long userId, Pageable pageable);

    /**
     * 공개 시험 목록 (페이징, 삭제된 것 제외)
     */
    Page<UserExam> findByVisibilityAndDeletedDateIsNull(ExamVisibility visibility, Pageable pageable);

    // ===== JPQL 쿼리 사용 =====

    /**
     * 날짜 범위로 검색 (시험 날짜 기준)
     */
    @Query("SELECT ue FROM UserExam ue " +
            "WHERE ue.examDate BETWEEN :startDate AND :endDate " +
            "AND ue.deletedDate IS NULL")
    List<UserExam> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * ID로 시험 조회 (문제 포함)
     */
    @Query("SELECT ue FROM UserExam ue " +
            "LEFT JOIN FETCH ue.examItems uei " +
            "WHERE ue.id = :examId " +
            "AND ue.deletedDate IS NULL")
    Optional<UserExam> findByIdWithItems(@Param("examId") Long examId);

    /**
     * 최근 생성된 시험 조회 (삭제된 것 제외)
     */
    @Query("SELECT ue FROM UserExam ue " +
            "WHERE ue.deletedDate IS NULL " +
            "ORDER BY ue.createdDate DESC")
    Page<UserExam> findRecentExams(Pageable pageable);

    /**
     * 생성자와 공개여부로 조회
     * - 본인이 만든 시험 또는 PUBLIC 시험
     * - createdBy는 Long 타입 userId
     */
    @Query("SELECT ue FROM UserExam ue " +
            "WHERE (ue.createdBy = :userId OR ue.visibility = 'PUBLIC') " +
            "AND ue.deletedDate IS NULL " +
            "ORDER BY ue.createdDate DESC")
    List<UserExam> findMyAndPublicExams(@Param("userId") Long userId);



    /**
     * 학교별 공개 시험 조회 (Teacher 엔티티 사용)
     * - Teacher 엔티티가 생성되면 주석 해제
     */
    // @Query("SELECT ue FROM UserExam ue " +
    //        "WHERE ue.visibility = 'SCHOOL' " +
    //        "AND ue.createdBy IN (SELECT t.userId FROM Teacher t WHERE t.school = :school) " +
    //        "AND ue.deletedDate IS NULL")
    // List<UserExam> findSchoolExams(@Param("school") String school);
}

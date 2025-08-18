package com.pullit.exam.repository;

import com.pullit.exam.entity.Exam;
import com.pullit.exam.enums.ExamVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long>, QuerydslPredicateExecutor<Exam> {

    // ===== 기본 검색 메서드 =====

    /**
     * 시험 이름으로 검색 (대소문자 무시, 부분 일치)
     */
    List<Exam> findByExamNameContainingIgnoreCase(String keyword);

    /**
     * 과목 ID로 검색
     * subject 엔티티의 subjectId 필드 사용
     */
    List<Exam> findBySubject_SubjectId(Long subjectId);

    /**
     * 대단원(largeChapter) 코드로 검색
     * Exam 엔티티는 largeChapter만 가지고 있음
     */
    List<Exam> findByLargeChapter_Code(Long largeChapterCode);

    /**
     * 공개 여부로 검색
     */
    List<Exam> findByVisibility(ExamVisibility visibility);

    /**
     * 생성자 ID로 검색 (Long 타입)
     */
    List<Exam> findByCreatedBy(Long userId);

    // ===== 복합 조건 검색 =====

    /**
     * 과목과 대단원으로 검색
     */
    List<Exam> findBySubject_SubjectIdAndLargeChapter_Code(Long subjectId, Long largeChapterCode);

    /**
     * 과목과 공개여부로 검색
     */
    List<Exam> findBySubject_SubjectIdAndVisibility(Long subjectId, ExamVisibility visibility);

    /**
     * 과목, 대단원, 공개여부로 검색
     */
    List<Exam> findBySubject_SubjectIdAndLargeChapter_CodeAndVisibility(
            Long subjectId,
            Long largeChapterCode,
            ExamVisibility visibility
    );

    // ===== JPQL 쿼리 사용 =====

    /**
     * ID로 시험 조회 (연관 엔티티 포함)
     * - subject 정보와 examItems를 함께 로드
     */
    @Query("SELECT e FROM Exam e " +
            "LEFT JOIN FETCH e.subject s " +
            "LEFT JOIN FETCH e.examItems ei " +
            "WHERE e.id = :examId")
    Optional<Exam> findByIdWithItems(@Param("examId") Long examId);

    /**
     * 학교별 공개 시험 조회 (Teacher 엔티티 사용)
     * - SCHOOL visibility인 경우 같은 학교 교사의 시험만 조회
     * - Teacher 엔티티가 생성되면 주석 해제
     */
    // @Query("SELECT e FROM Exam e " +
    //        "WHERE e.visibility = 'SCHOOL' " +
    //        "AND e.createdBy IN (SELECT t.userId FROM Teacher t WHERE t.school = :school)")
    // List<Exam> findSchoolExams(@Param("school") String school);

    /**
     * 공개 시험 조회 (PUBLIC만)
     */
    @Query("SELECT e FROM Exam e WHERE e.visibility = 'PUBLIC'")
    List<Exam> findPublicExams();

    /**
     * 생성자와 공개여부로 조회
     * - 본인이 만든 시험 또는 PUBLIC 시험
     * - createdBy는 Long 타입 userId
     */
    @Query("SELECT e FROM Exam e " +
            "WHERE e.createdBy = :userId " +
            "OR e.visibility = 'PUBLIC'")
    List<Exam> findMyAndPublicExams(@Param("userId") Long userId);
}

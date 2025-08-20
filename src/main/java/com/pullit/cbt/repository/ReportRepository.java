package com.pullit.cbt.repository;

import com.pullit.cbt.entity.AttemptExam;
import com.pullit.cbt.projection.DetailDifficultyProjection;
import com.pullit.cbt.projection.DetailErrataProjection;
import com.pullit.cbt.projection.DetailEvaluationProjection;
import com.pullit.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<AttemptExam, Long> {

    // 과목 코드별 시험 응시 리스트
    @Query("""
        SELECT ae
        FROM AttemptExam ae
        JOIN FETCH ae.exam e
        JOIN FETCH ae.user u
        LEFT JOIN FETCH ae.attemptQuestions q
        LEFT JOIN FETCH q.examItem i
        WHERE e.areaCode = :areaCode
          AND u.id = :userId
    """)
    List<AttemptExam> findByAreaCodeAndUserId(
            @Param("areaCode") String areaCode,
            @Param("userId") Long userId
    );

    // 상세 리포트 정오표
    @Query(value = """
            SELECT
                uei.user_exam_id,
                eaq_user.user_id,
                uei.item_id,
                ed.domain_name,
                uei.item_order,
                uei.points,
                iht.answer,
                eaq_user.user_answer,
                eaq_user.is_correct,
                eaq_user.user_points,
                eaq_avg.accuracy
            FROM user_exam_items uei
             LEFT JOIN (
                SELECT question_id, AVG(is_correct) AS accuracy
                FROM exam_attempt_question
                LEFT JOIN exam_attempt e on exam_attempt_question.attempt_id = e.attempt_id
                WHERE e.exam_id = :examId
                GROUP BY question_id
            ) eaq_avg ON eaq_avg.question_id = uei.id
            LEFT JOIN item_metadata im ON im.item_id = uei.item_id
            JOIN item_activity_mapping iam on im.item_id = iam.item_id
            JOIN evaluation_domains ed on iam.activity_category_id = ed.domain_id
            LEFT JOIN item_html_data iht ON iht.item_id = im.item_id
            LEFT JOIN (
                SELECT
                    eaq.user_answer,
                    eaq.question_id,
                    eaq.points AS user_points,
                    eaq.is_correct,
                    ea.user_id
                FROM exam_attempt_question eaq
                JOIN exam_attempt ea ON eaq.attempt_id = ea.attempt_id
                WHERE ea.user_id = :userId
            ) eaq_user ON eaq_user.question_id = uei.id
            WHERE uei.user_exam_id = :examId;
    """, nativeQuery = true)
    List<DetailErrataProjection> findDetailErrata(
            @Param("examId") Long examId,
            @Param("userId") Long userId
    );

    // 문항 난이도별 성취율+평균 정답율
    @Query(value = """
    SELECT
        q.difficulty_code AS difficultyCode,
        COUNT(*) AS itemCount,
        SUM(q.sum_correct) * 1.0 / SUM(q.attempts) AS totalAvg,
        SUM(q.sum_correct_user) * 1.0 / SUM(q.attempts_user) AS userAvg
    FROM (
        SELECT
            im.difficulty_code,
            SUM(eaq.is_correct) AS sum_correct,
            COUNT(eaq.is_correct) AS attempts,
            SUM(CASE WHEN ea.user_id = :userId AND ea.exam_id = :examId THEN eaq.is_correct ELSE 0 END) AS sum_correct_user,
            SUM(CASE WHEN ea.user_id = :userId AND ea.exam_id = :examId THEN 1 ELSE 0 END) AS attempts_user
        FROM user_exam_items uei
        JOIN item_metadata im ON uei.item_id = im.item_id
        LEFT JOIN exam_attempt_question eaq ON eaq.question_id = uei.id
        LEFT JOIN exam_attempt ea ON ea.attempt_id = eaq.attempt_id
        WHERE uei.user_exam_id = :examId
        GROUP BY uei.id, im.difficulty_code
    ) q
    GROUP BY q.difficulty_code
    ORDER BY q.difficulty_code
    """, nativeQuery = true)
    List<DetailDifficultyProjection> findDetailDifficultyByExamId(
            @Param("userId") Long userId,
            @Param("examId") Long examId
    );

    @Query(value = """
    
    """, nativeQuery = true)
    List<DetailEvaluationProjection> findDetailEvaluationByExamId(
            @Param("userId") Long userId,
            @Param("examId") Long examId
    );
}

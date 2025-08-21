package com.pullit.cbt.repository;

import com.pullit.cbt.entity.AttemptExam;

import com.pullit.exam.entity.UserExamItem;
import com.pullit.item.entity.ItemMetadata;
import com.pullit.item.entity.ItemHtmlData;

import com.pullit.cbt.projection.DetailDifficultyProjection;
import com.pullit.cbt.projection.DetailErrataProjection;
import com.pullit.cbt.projection.DetailEvaluationProjection;
import com.pullit.user.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
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


    @Query("""
        SELECT ae
        FROM AttemptExam ae
        JOIN FETCH ae.exam e
        JOIN FETCH ae.user u
        LEFT JOIN FETCH ae.attemptQuestions q
        LEFT JOIN FETCH q.examItem i
        WHERE ae.id = :attemptId
          AND u.id = :userId
    """)
    Optional<AttemptExam> findByAttemptIdAndUserId(
            @Param("attemptId") Long attemptId,
            @Param("userId") Long userId
    );

    // ===== CBT 문항 조회 관련 메서드 =====

    /**
     * question_id로 UserExamItem 조회
     */
    @Query("SELECT uei FROM UserExamItem uei WHERE uei.id = :questionId")
    UserExamItem findUserExamItemById(@Param("questionId") Long questionId);

    /**
     * item_id로 ItemMetadata와 HTML 데이터를 함께 조회
     */
    @Query("SELECT i FROM ItemMetadata i " +
            "LEFT JOIN FETCH i.htmlData " +
            "WHERE i.itemId = :itemId")
    Optional<ItemMetadata> findItemMetadataWithHtmlData(@Param("itemId") Long itemId);

    // 상세 리포트 정오표
    @Query(value = """
            SELECT
                uei.user_exam_id,
                eaq_user.user_id,
                uei.item_id,
                eaq_user.question_id,
                eaq_user.duration,
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
                    eaq.duration,
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

    // 문항 난이도별 성취율+평균 정답율+평균 소요시간
    @Query(value = """
        WITH total_points_by_difficulty AS (
            SELECT
                im.difficulty_code,
                SUM(COALESCE(uei.points, 0)) AS total_points
            FROM user_exam_items uei
                     JOIN item_metadata im ON uei.item_id = im.item_id
            WHERE uei.user_exam_id = :examId
            GROUP BY im.difficulty_code
        ),
             user_difficulty_totals AS (
                 SELECT
                     im.difficulty_code,
                     ea.user_id,
                     SUM(COALESCE(eaq.points, 0)) AS user_total_points
                 FROM user_exam_items uei
                          JOIN item_metadata im ON uei.item_id = im.item_id
                          LEFT JOIN exam_attempt_question eaq ON eaq.question_id = uei.id
                          LEFT JOIN exam_attempt ea ON ea.attempt_id = eaq.attempt_id
                 WHERE uei.user_exam_id = :examId
                   AND ea.exam_id = :examId
                 GROUP BY im.difficulty_code, ea.user_id
             ),
             user_difficulty_correct_counts AS (
                 SELECT
                     im.difficulty_code,
                     ea.user_id,
                     SUM(COALESCE(eaq.is_correct, 0)) AS user_correct_count
                 FROM user_exam_items uei
                          JOIN item_metadata im ON uei.item_id = im.item_id
                          LEFT JOIN exam_attempt_question eaq ON eaq.question_id = uei.id
                          LEFT JOIN exam_attempt ea ON ea.attempt_id = eaq.attempt_id
                 WHERE uei.user_exam_id = :examId
                   AND ea.exam_id = :examId
                 GROUP BY im.difficulty_code, ea.user_id
             ),
             user_difficulty_duration AS (
                 SELECT
                     im.difficulty_code,
                     ea.user_id,
                     AVG(COALESCE(eaq.duration, 0)) AS avg_duration
                 FROM user_exam_items uei
                          JOIN item_metadata im ON uei.item_id = im.item_id
                          LEFT JOIN exam_attempt_question eaq ON eaq.question_id = uei.id
                          LEFT JOIN exam_attempt ea ON ea.attempt_id = eaq.attempt_id
                 WHERE uei.user_exam_id = :examId
                   AND ea.exam_id = :examId
                 GROUP BY im.difficulty_code, ea.user_id
             ),
             difficulty_stats AS (
                 SELECT
                     im.difficulty_code,
                     uei.id as item_id,
                     COALESCE(uei.points, 0) AS item_points,
                     SUM(COALESCE(eaq.is_correct, 0)) AS sum_correct,
                     COUNT(CASE WHEN eaq.is_correct IS NOT NULL THEN 1 END) AS attempts,
                     SUM(CASE
                             WHEN ea.user_id = :userId AND ea.exam_id = :examId
                                 THEN COALESCE(eaq.is_correct, 0)
                             ELSE 0
                         END) AS sum_correct_user,
                     SUM(CASE
                             WHEN ea.user_id = :userId AND ea.exam_id = :examId
                                 THEN 1
                             ELSE 0
                         END) AS attempts_user
                 FROM user_exam_items uei
                          JOIN item_metadata im ON uei.item_id = im.item_id
                          LEFT JOIN exam_attempt_question eaq ON eaq.question_id = uei.id
                          LEFT JOIN exam_attempt ea ON ea.attempt_id = eaq.attempt_id
                 WHERE uei.user_exam_id = :examId
                 GROUP BY uei.id, im.difficulty_code, uei.points
             )
        SELECT
            ds.difficulty_code AS difficultyCode,
            COUNT(DISTINCT ds.item_id) AS itemCount,
            COALESCE(tp.total_points, 0) AS totalPoints,
            COALESCE(MAX(udt_user.user_total_points), 0) AS userPoints,
            CASE
                WHEN COUNT(udt.user_id) > 0 THEN
                    AVG(udt.user_total_points)
                ELSE 0
                END AS avgPoints,
            CASE
                WHEN COUNT(udcc.user_id) > 0 THEN
                    AVG(udcc.user_correct_count)
                ELSE 0
                END AS avgCount,
            COALESCE(MAX(udcc_user.user_correct_count), 0) AS userCount,
            COALESCE(MAX(udd_user.avg_duration), 0) AS userDuration,
            CASE
                WHEN COUNT(udd.user_id) > 0 THEN
                    AVG(udd.avg_duration)
                ELSE 0
                END AS avgDuration
        FROM difficulty_stats ds
                 LEFT JOIN total_points_by_difficulty tp ON ds.difficulty_code = tp.difficulty_code
                 LEFT JOIN user_difficulty_totals udt ON ds.difficulty_code = udt.difficulty_code
                 LEFT JOIN user_difficulty_totals udt_user ON ds.difficulty_code = udt_user.difficulty_code
            AND udt_user.user_id = :userId
                 LEFT JOIN user_difficulty_correct_counts udcc ON ds.difficulty_code = udcc.difficulty_code
                 LEFT JOIN user_difficulty_correct_counts udcc_user ON ds.difficulty_code = udcc_user.difficulty_code
            AND udcc_user.user_id = :userId
                 LEFT JOIN user_difficulty_duration udd ON ds.difficulty_code = udd.difficulty_code
                 LEFT JOIN user_difficulty_duration udd_user ON ds.difficulty_code = udd_user.difficulty_code
            AND udd_user.user_id = :userId
        GROUP BY ds.difficulty_code
        ORDER BY ds.difficulty_code;
    """, nativeQuery = true)
    List<DetailDifficultyProjection> findDetailDifficultyByExamId(
            @Param("userId") Long userId,
            @Param("examId") Long examId
    );

    @Query(value = """
        WITH domain_totals AS (
            SELECT
                ed.domain_name,
                COUNT(*) AS total_count
            FROM user_exam_items uei
                     JOIN item_metadata im ON uei.item_id = im.item_id
                     JOIN item_activity_mapping iam ON im.item_id = iam.item_id
                     JOIN evaluation_domains ed ON iam.activity_category_id = ed.domain_id
            WHERE user_exam_id = 6
            GROUP BY ed.domain_name
        ),
        user_domain_correct_counts AS (
            SELECT
                ed.domain_name,
                ea.user_id,
                SUM(COALESCE(eaq.is_correct, 0)) AS user_correct_count
            FROM user_exam_items uei
                     JOIN item_metadata im ON uei.item_id = im.item_id
                     JOIN item_activity_mapping iam ON im.item_id = iam.item_id
                     JOIN evaluation_domains ed ON iam.activity_category_id = ed.domain_id
                     JOIN exam_attempt_question eaq ON uei.id = eaq.question_id
                     JOIN exam_attempt ea ON ea.attempt_id = eaq.attempt_id
            WHERE uei.user_exam_id = 6
            GROUP BY ed.domain_name, ea.user_id
        ),
        user_domain_points AS (
            SELECT
                ed.domain_name,
                ea.user_id,
                SUM(COALESCE(eaq.points, 0)) AS user_total_points
            FROM user_exam_items uei
                     JOIN item_metadata im ON uei.item_id = im.item_id
                     JOIN item_activity_mapping iam ON im.item_id = iam.item_id
                     JOIN evaluation_domains ed ON iam.activity_category_id = ed.domain_id
                     JOIN exam_attempt_question eaq ON uei.id = eaq.question_id
                     JOIN exam_attempt ea ON ea.attempt_id = eaq.attempt_id
            WHERE uei.user_exam_id = 6
            GROUP BY ed.domain_name, ea.user_id
        ),
        user_domain_duration AS (
            SELECT
                ed.domain_name,
                ea.user_id,
                AVG(COALESCE(eaq.duration, 0)) AS avg_duration
            FROM user_exam_items uei
                     JOIN item_metadata im ON uei.item_id = im.item_id
                     JOIN item_activity_mapping iam ON im.item_id = iam.item_id
                     JOIN evaluation_domains ed ON iam.activity_category_id = ed.domain_id
                     JOIN exam_attempt_question eaq ON uei.id = eaq.question_id
                     JOIN exam_attempt ea ON ea.attempt_id = eaq.attempt_id
            WHERE uei.user_exam_id = 6
            GROUP BY ed.domain_name, ea.user_id
        )
        SELECT
            udcc.domain_name AS domainName,
            dt.total_count AS totalCount,
            MAX(CASE WHEN udcc.user_id = 8 THEN udcc.user_correct_count ELSE 0 END) AS userCount,
            AVG(udcc.user_correct_count) AS avgCount,
            MAX(CASE WHEN udp.user_id = 8 THEN udp.user_total_points ELSE 0 END) AS userPoints,
            AVG(udp.user_total_points) AS avgPoints,
            MAX(CASE WHEN udd.user_id = 8 THEN udd.avg_duration ELSE 0 END) AS userDuration,
            AVG(udd.avg_duration) AS avgDuration
        FROM user_domain_correct_counts udcc
                 JOIN domain_totals dt ON udcc.domain_name = dt.domain_name
                 LEFT JOIN user_domain_points udp ON udcc.domain_name = udp.domain_name AND udcc.user_id = udp.user_id
                 LEFT JOIN user_domain_duration udd ON udcc.domain_name = udd.domain_name AND udcc.user_id = udd.user_id
        GROUP BY udcc.domain_name, dt.total_count
        ORDER BY udcc.domain_name;
    """, nativeQuery = true)
    List<DetailEvaluationProjection> findDetailEvaluationByExamId(
            @Param("userId") Long userId,
            @Param("examId") Long examId
    );


}


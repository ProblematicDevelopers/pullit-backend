package com.pullit.classes.repository;

import com.pullit.classes.Projection.StatsDetailProjection;
import com.pullit.classes.Projection.StatsLineProjection;
import com.pullit.classes.entity.Classes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatsRepository extends JpaRepository<Classes, Long> {


    // 클래스 내 시험별 평균 점수/내 점수 조회
    @Query(value = """
        WITH latest_attempts AS (
           SELECT
               ea.exam_id,
               ue.exam_name,
               ea.user_id,
               ea.attempt_id,
               ue.total_points,
               SUM(eaq.points) as score,
               ROW_NUMBER() OVER (
                   PARTITION BY ea.user_id, ea.exam_id
                   ORDER BY ea.completed_at DESC
                   ) as rn
           FROM classes c
                    JOIN students s ON c.class_id = s.class_group_id
                    JOIN exam_attempt ea ON s.user_id = ea.user_id
                    JOIN user_exams ue ON ea.exam_id = ue.exam_id
                    JOIN exam_attempt_question eaq ON ea.attempt_id = eaq.attempt_id
           WHERE ue.class_id = :classId
             AND ea.completed_at IS NOT NULL
           GROUP BY ea.exam_id, ea.user_id, ea.attempt_id
       ),
       class_stats AS (
           SELECT
               exam_id,
               AVG(score) as class_avg,
               MAX(score) as class_max,
               MIN(score) as class_min
           FROM latest_attempts
           WHERE rn = 1
           GROUP BY exam_id
       )
      SELECT
          cs.exam_id,
          la.exam_name,
          la.total_points,
          COALESCE(la.score, 0) as userPoints,
          ROUND(cs.class_avg, 2) as avgPoints,
          class_max as maxPoints,
          class_min as minPoints
      FROM class_stats cs
               LEFT JOIN latest_attempts la ON cs.exam_id = la.exam_id
          AND la.user_id = :userId
          AND la.rn = 1
        """, nativeQuery = true
    )
    public List<StatsLineProjection> findStatsLines(Long userId, Long classId);


    @Query(value = """
        WITH latest_attempts AS (
          SELECT
              ea.exam_id,
              ea.user_id,
              ea.attempt_id,
              ea.completed_at,
              ue.exam_name,
              SUM(eaq.points) as score,
              ROW_NUMBER() OVER (
                  PARTITION BY ea.user_id, ea.exam_id
                  ORDER BY ea.completed_at DESC
                  ) as rn
          FROM classes c
                   JOIN students s ON c.class_id = s.class_group_id
                   JOIN exam_attempt ea ON s.user_id = ea.user_id
                   JOIN user_exams ue ON ea.exam_id = ue.exam_id
                   JOIN exam_attempt_question eaq ON ea.attempt_id = eaq.attempt_id
          WHERE ue.class_id = :classId
            AND ea.completed_at IS NOT NULL
          GROUP BY ea.exam_id, ea.user_id, ea.attempt_id, ea.completed_at, ue.exam_name
          ),
           exam_scores AS (
               SELECT
                   exam_id,
                   user_id,
                   exam_name,
                   score
               FROM latest_attempts
               WHERE rn = 1
           ),
           exam_scores_with_median AS (
               SELECT
                   exam_id,
                   user_id,
                   exam_name,
                   score,
                   COUNT(*) OVER (PARTITION BY exam_id) as total_count,
                   ROW_NUMBER() OVER (PARTITION BY exam_id ORDER BY score) as row_num
               FROM exam_scores
           ),
           exam_medians AS (
               SELECT
                   exam_id,
                   exam_name,
                   AVG(score) as median_score
               FROM exam_scores_with_median
               WHERE row_num IN (
                                 FLOOR((total_count + 1) / 2),
                                 CEIL((total_count + 1) / 2)
                   )
               GROUP BY exam_id, exam_name
           ),
           exam_quartiles AS (
               SELECT
                   exam_id,
                   exam_name,
                   AVG(CASE WHEN row_num IN (
                                             FLOOR((total_count + 1) * 0.25),
                                             CEIL((total_count + 1) * 0.25)
                       ) THEN score END) as q1,
                   AVG(CASE WHEN row_num IN (
                                             FLOOR((total_count + 1) * 0.75),
                                             CEIL((total_count + 1) * 0.75)
                       ) THEN score END) as q3
               FROM exam_scores_with_median
               GROUP BY exam_id, exam_name
           ),
           exam_stats AS (
               SELECT
                   exam_id,
                   exam_name,
                   AVG(score) as avg_score,
                   MAX(score) as max_score,
                   MIN(score) as min_score,
                   STDDEV(score) as std_deviation,
                   COUNT(*) as total_students
               FROM exam_scores es
               GROUP BY exam_id, exam_name
           ),
           exam_rankings AS (
               SELECT
                   es.exam_id,
                   es.user_id,
                   es.exam_name,
                   es.score,
                   RANK() OVER (PARTITION BY es.exam_id ORDER BY es.score DESC) as rank_position,
                   COUNT(*) OVER (PARTITION BY es.exam_id) as total_students,
                   ROUND(
                           PERCENT_RANK() OVER (PARTITION BY es.exam_id ORDER BY es.score) * 100, 1
                   ) as percentile,
                   NTILE(4) OVER (PARTITION BY es.exam_id ORDER BY es.score) as quartile
               FROM exam_scores es
           )
          SELECT
              er.exam_id,
              er.exam_name,
              er.score as score,
              er.rank_position,
              er.total_students,
              er.percentile,
              er.quartile,
              CASE
                  WHEN er.quartile = 1 THEN '하위 25%'
                  WHEN er.quartile = 2 THEN '하위 25-50%'
                  WHEN er.quartile = 3 THEN '상위 25-50%'
                  WHEN er.quartile = 4 THEN '상위 25%'
                  END as quartile_description,
              ROUND(
                  (er.rank_position - 1) / er.total_students * 100, 1
              ) as top_percentage,
              ROUND(em.median_score, 2) as median,
              ROUND(est.avg_score, 2) as mean,
              est.max_score as max,
              est.min_score as min,
              ROUND(est.std_deviation, 2) as std_deviation,
              -- 사분위수 추가
              ROUND(eq.q1, 2) as q1,
              ROUND(eq.q3, 2) as q3
          FROM exam_rankings er
               JOIN exam_stats est ON er.exam_id = est.exam_id
               JOIN exam_medians em ON er.exam_id = em.exam_id
               JOIN exam_quartiles eq ON er.exam_id = eq.exam_id
          WHERE er.user_id = :userId
          ORDER BY er.exam_id
        """, nativeQuery = true)
    public List<StatsDetailProjection> findStatsDetail(
            Long userId,
            Long classId
    );
}

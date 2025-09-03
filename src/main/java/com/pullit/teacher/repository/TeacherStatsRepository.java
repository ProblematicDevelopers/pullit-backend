package com.pullit.teacher.repository;

import com.pullit.classes.entity.Classes;
import com.pullit.teacher.projection.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherStatsRepository extends JpaRepository<Classes, Long> {
    
    // 교사 권한 확인
    @Query(value = """
        SELECT COUNT(*)
        FROM classes c
        WHERE c.class_id = :classId
        AND (c.teacher_id = :teacherId 
            OR EXISTS (
                SELECT 1 FROM class_teachers ct 
                WHERE ct.class_id = :classId 
                AND ct.teacher_id = :teacherId
            ))
        """, nativeQuery = true)
    Long countTeacherOfClass(@Param("teacherId") Long teacherId, @Param("classId") Long classId);
    
    // 클래스의 학생 수
    @Query(value = """
        SELECT COUNT(DISTINCT s.user_id)
        FROM students s
        WHERE s.class_group_id = :classId
        """, nativeQuery = true)
    Integer countStudentsByClassId(@Param("classId") Long classId);
    
    // 클래스의 시험 수
    @Query(value = """
        SELECT COUNT(DISTINCT ue.exam_id)
        FROM user_exams ue
        WHERE ue.class_id = :classId
        """, nativeQuery = true)
    Integer countExamsByClassId(@Param("classId") Long classId);
    
    // 클래스 전체 통계
    @Query(value = """
        WITH class_scores AS (
            SELECT 
                ea.user_id,
                ea.exam_id,
                SUM(eaq.points) as score,
                ea.completed_at,
                ROW_NUMBER() OVER (PARTITION BY ea.user_id, ea.exam_id ORDER BY ea.completed_at DESC) as rn
            FROM exam_attempt ea
            JOIN user_exams ue ON ea.exam_id = ue.exam_id
            JOIN exam_attempt_question eaq ON ea.attempt_id = eaq.attempt_id
            WHERE ue.class_id = :classId
            AND ea.status = 'DONE'
            GROUP BY ea.user_id, ea.exam_id, ea.attempt_id, ea.completed_at
        )
        SELECT 
            AVG(score) as averageScore,
            AVG(score) as medianScore,
            MAX(score) as highestScore,
            MIN(score) as lowestScore,
            MAX(completed_at) as lastExamDate
        FROM class_scores
        WHERE rn = 1
        """, nativeQuery = true)
    ClassOverallStatisticsProjection getClassOverallStatistics(@Param("classId") Long classId);
    
    // 최근 시험 요약
    @Query(value = """
        WITH exam_scores AS (
            SELECT 
                ue.exam_id,
                ue.exam_name,
                ea.completed_at as examDate,
                SUM(eaq.points) as score,
                ROW_NUMBER() OVER (PARTITION BY ea.user_id, ea.exam_id ORDER BY ea.completed_at DESC) as rn
            FROM exam_attempt ea
            JOIN user_exams ue ON ea.exam_id = ue.exam_id
            JOIN exam_attempt_question eaq ON ea.attempt_id = eaq.attempt_id
            WHERE ue.class_id = :classId
            AND ea.status = 'DONE'
            GROUP BY ue.exam_id, ue.exam_name, ea.user_id, ea.attempt_id, ea.completed_at
        )
        SELECT 
            exam_id as examId,
            exam_name as examName,
            MAX(examDate) as examDate,
            AVG(score) as averageScore,
            COUNT(DISTINCT score) as participantCount
        FROM exam_scores
        WHERE rn = 1
        GROUP BY exam_id, exam_name
        ORDER BY MAX(examDate) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<RecentExamSummaryProjection> getRecentExamSummaries(@Param("classId") Long classId, @Param("limit") int limit);
    
    // 성적 등급별 분포
    @Query(value = """
        WITH latest_scores AS (
            SELECT 
                ea.user_id,
                (SUM(eaq.points) * 100.0 / ue.total_points) as percentage,
                ROW_NUMBER() OVER (PARTITION BY ea.user_id, ea.exam_id ORDER BY ea.completed_at DESC) as rn
            FROM exam_attempt ea
            JOIN user_exams ue ON ea.exam_id = ue.exam_id
            JOIN exam_attempt_question eaq ON ea.attempt_id = eaq.attempt_id
            WHERE ue.class_id = :classId
            AND ea.status = 'DONE'
            GROUP BY ea.user_id, ea.exam_id, ea.attempt_id, ea.completed_at, ue.total_points
        )
        SELECT 
            SUM(CASE WHEN percentage >= 90 THEN 1 ELSE 0 END) as excellent,
            SUM(CASE WHEN percentage >= 80 AND percentage < 90 THEN 1 ELSE 0 END) as good,
            SUM(CASE WHEN percentage >= 70 AND percentage < 80 THEN 1 ELSE 0 END) as average,
            SUM(CASE WHEN percentage >= 60 AND percentage < 70 THEN 1 ELSE 0 END) as belowAverage,
            SUM(CASE WHEN percentage < 60 THEN 1 ELSE 0 END) as poor
        FROM latest_scores
        WHERE rn = 1
        """, nativeQuery = true)
    GradeRangeDistributionProjection getGradeRangeDistribution(@Param("classId") Long classId);
    
    // 학생별 전체 성적 조회
    @Query(value = """
        WITH student_scores AS (
            SELECT 
                s.user_id as studentId,
                u.full_name as studentName,
                s.student_no as studentNo,
                ea.exam_id,
                ue.exam_name as examName,
                SUM(eaq.points) as score,
                ue.total_points as totalPoints,
                (SUM(eaq.points) * 100.0 / ue.total_points) as percentage,
                ROW_NUMBER() OVER (PARTITION BY s.user_id, ea.exam_id ORDER BY ea.completed_at DESC) as attempt_rn,
                RANK() OVER (PARTITION BY ea.exam_id ORDER BY SUM(eaq.points) DESC) as examRank,
                COUNT(*) OVER (PARTITION BY ea.exam_id) as totalStudents
            FROM students s
            JOIN users u ON s.user_id = u.id
            LEFT JOIN exam_attempt ea ON s.user_id = ea.user_id
            LEFT JOIN user_exams ue ON ea.exam_id = ue.exam_id
            LEFT JOIN exam_attempt_question eaq ON ea.attempt_id = eaq.attempt_id
            WHERE s.class_group_id = :classId
            AND (ea.status = 'DONE' OR ea.status IS NULL)
            AND (ue.class_id = :classId OR ue.class_id IS NULL)
            GROUP BY s.user_id, u.full_name, s.student_no, ea.exam_id, ue.exam_name, 
                     ea.attempt_id, ea.completed_at, ue.total_points
        ),
        student_summary AS (
            SELECT 
                studentId,
                studentName,
                studentNo,
                AVG(percentage) as averageScore,
                COUNT(DISTINCT exam_id) as totalExamsTaken,
                AVG(examRank) as avgRank,
                AVG((totalStudents - examRank + 1) * 100.0 / totalStudents) as percentile
            FROM student_scores
            WHERE attempt_rn = 1
            GROUP BY studentId, studentName, studentNo
        )
        SELECT 
            ss.*,
            RANK() OVER (ORDER BY ss.averageScore DESC) as classRank,
            CASE 
                WHEN LAG(ss.averageScore) OVER (ORDER BY ss.studentId) < ss.averageScore THEN 'IMPROVING'
                WHEN LAG(ss.averageScore) OVER (ORDER BY ss.studentId) > ss.averageScore THEN 'DECLINING'
                ELSE 'STABLE'
            END as trend
        FROM student_summary ss
        ORDER BY ss.averageScore DESC
        """, nativeQuery = true)
    List<StudentGradeProjection> getAllStudentGrades(@Param("classId") Long classId);

    // 윈도우 함수 미지원 DB 호환용: 학생별 전체 성적 조회 (기본)
    @Query(value = """
        SELECT 
            s.user_id as studentId,
            u.full_name as studentName,
            s.student_no as studentNo,
            AVG((sa.score * 100.0) / sa.total_points) as averageScore,
            COUNT(DISTINCT sa.exam_id) as totalExamsTaken,
            NULL as avgRank,
            NULL as percentile,
            NULL as classRank,
            'STABLE' as trend
        FROM students s
        JOIN users u ON s.user_id = u.id
        LEFT JOIN (
            SELECT 
                ea.user_id,
                ea.exam_id,
                SUM(eaq.points) as score,
                ue.total_points
            FROM exam_attempt ea
            JOIN user_exams ue ON ea.exam_id = ue.exam_id
            JOIN exam_attempt_question eaq ON ea.attempt_id = eaq.attempt_id
            JOIN (
                SELECT ea2.user_id, ea2.exam_id, MAX(ea2.completed_at) as max_completed_at
                FROM exam_attempt ea2
                JOIN user_exams ue2 ON ea2.exam_id = ue2.exam_id
                WHERE ue2.class_id = :classId
                  AND ea2.status = 'DONE'
                GROUP BY ea2.user_id, ea2.exam_id
            ) latest ON latest.user_id = ea.user_id 
                   AND latest.exam_id = ea.exam_id 
                   AND latest.max_completed_at = ea.completed_at
            WHERE ea.status = 'DONE'
            GROUP BY ea.user_id, ea.exam_id, ue.total_points
        ) sa ON sa.user_id = s.user_id
        WHERE s.class_group_id = :classId
        GROUP BY s.user_id, u.full_name, s.student_no
        ORDER BY averageScore DESC
        """, nativeQuery = true)
    List<StudentGradeProjection> getAllStudentGradesBasic(@Param("classId") Long classId);
    
    // 특정 시험의 학생별 성적
    @Query(value = """
        WITH exam_scores AS (
            SELECT 
                s.user_id as studentId,
                u.full_name as studentName,
                s.student_no as studentNo,
                SUM(eaq.points) as score,
                ue.total_points as totalPoints,
                (SUM(eaq.points) * 100.0 / ue.total_points) as percentage,
                RANK() OVER (ORDER BY SUM(eaq.points) DESC) as exam_rank,
                COUNT(*) OVER () as totalStudents,
                ea.completed_at
            FROM students s
            JOIN users u ON s.user_id = u.id
            JOIN exam_attempt ea ON s.user_id = ea.user_id
            JOIN user_exams ue ON ea.exam_id = ue.exam_id
            JOIN exam_attempt_question eaq ON ea.attempt_id = eaq.attempt_id
            WHERE s.class_group_id = :classId
            AND ea.exam_id = :examId
            AND ea.status = 'DONE'
            GROUP BY s.user_id, u.full_name, s.student_no, ea.attempt_id, 
                     ea.completed_at, ue.total_points
        )
        SELECT 
            *,
            CASE 
                WHEN percentage >= 90 THEN 'A'
                WHEN percentage >= 80 THEN 'B'
                WHEN percentage >= 70 THEN 'C'
                WHEN percentage >= 60 THEN 'D'
                ELSE 'F'
            END as grade
        FROM exam_scores
        ORDER BY exam_rank
        """, nativeQuery = true)
    List<StudentGradeByExamProjection> getStudentGradesByExam(@Param("classId") Long classId, @Param("examId") Long examId);
    
    // 시험 기본 정보
    @Query(value = """
        SELECT 
            ue.exam_id as examId,
            ue.exam_name as examName,
            ue.created_at as examDate,
            ue.total_points as totalPoints
        FROM user_exams ue
        WHERE ue.exam_id = :examId
        """, nativeQuery = true)
    ExamBasicInfoProjection getExamBasicInfo(@Param("examId") Long examId);
    
    // 시험 통계
    @Query(value = """
        WITH exam_scores AS (
            SELECT 
                SUM(eaq.points) as score,
                ue.total_points as totalPoints
            FROM exam_attempt ea
            JOIN user_exams ue ON ea.exam_id = ue.exam_id
            JOIN exam_attempt_question eaq ON ea.attempt_id = eaq.attempt_id
            JOIN students s ON ea.user_id = s.user_id
            WHERE s.class_group_id = :classId
            AND ea.exam_id = :examId
            AND ea.status = 'DONE'
            GROUP BY ea.attempt_id, ue.total_points
        )
        SELECT 
            AVG(score) as averageScore,
            AVG(score) as medianScore,
            STDDEV(score) as standardDeviation,
            MAX(score) as highestScore,
            MIN(score) as lowestScore,
            (SUM(CASE WHEN score >= totalPoints * 0.6 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) as passRate,
            (SUM(CASE WHEN score >= totalPoints * 0.9 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) as excellentRate
        FROM exam_scores
        """, nativeQuery = true)
    ExamStatisticsProjection getExamStatistics(@Param("classId") Long classId, @Param("examId") Long examId);
    
    // 학생별 시험 결과
    @Query(value = """
        SELECT 
            s.user_id as studentId,
            u.full_name as studentName,
            s.student_no as studentNo,
            SUM(eaq.points) as score,
            (SUM(eaq.points) * 100.0 / ue.total_points) as percentage,
            RANK() OVER (ORDER BY SUM(eaq.points) DESC) as exam_rank,
            CASE 
                WHEN (SUM(eaq.points) * 100.0 / ue.total_points) >= 90 THEN 'A'
                WHEN (SUM(eaq.points) * 100.0 / ue.total_points) >= 80 THEN 'B'
                WHEN (SUM(eaq.points) * 100.0 / ue.total_points) >= 70 THEN 'C'
                WHEN (SUM(eaq.points) * 100.0 / ue.total_points) >= 60 THEN 'D'
                ELSE 'F'
            END as grade,
            ea.completed_at as completedAt,
            TIMESTAMPDIFF(MINUTE, ea.started_at, ea.completed_at) as timeTaken
        FROM students s
        JOIN users u ON s.user_id = u.id
        JOIN exam_attempt ea ON s.user_id = ea.user_id
        JOIN user_exams ue ON ea.exam_id = ue.exam_id
        JOIN exam_attempt_question eaq ON ea.attempt_id = eaq.attempt_id
        WHERE s.class_group_id = :classId
        AND ea.exam_id = :examId
        AND ea.status = 'DONE'
        GROUP BY s.user_id, u.full_name, s.student_no, ea.attempt_id, 
                 ea.started_at, ea.completed_at, ue.total_points
        ORDER BY exam_rank
        """, nativeQuery = true)
    List<StudentResultProjection> getStudentResultsByExam(@Param("classId") Long classId, @Param("examId") Long examId);
    
    // 문제별 정답률
    @Query(value = """
        SELECT 
            eaq.question_number as questionNumber,
            'MULTIPLE_CHOICE' as questionType,
            SUM(CASE WHEN eaq.is_correct THEN 1 ELSE 0 END) as correctCount,
            (SUM(CASE WHEN eaq.is_correct THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) as correctRate,
            MAX(eaq.points) as points
        FROM exam_attempt_question eaq
        JOIN exam_attempt ea ON eaq.attempt_id = ea.attempt_id
        WHERE ea.exam_id = :examId
        AND ea.status = 'DONE'
        GROUP BY eaq.question_number
        ORDER BY eaq.question_number
        """, nativeQuery = true)
    List<QuestionStatisticsProjection> getQuestionStatistics(@Param("examId") Long examId);
    
    // 학생 기본 정보
    @Query(value = """
        SELECT 
            s.user_id as studentId,
            u.full_name as studentName,
            s.student_no as studentNo,
            s.grade_name as grade,
            sch.school_name as schoolName
        FROM students s
        JOIN users u ON s.user_id = u.id
        LEFT JOIN schools sch ON s.school_id = sch.id
        WHERE s.user_id = :studentId
        """, nativeQuery = true)
    StudentBasicInfoProjection getStudentBasicInfo(@Param("studentId") Long studentId);
    
    // 학생 성적 요약
    @Query(value = """
        WITH student_scores AS (
            SELECT 
                ea.exam_id,
                SUM(eaq.points) as score,
                ue.total_points,
                (SUM(eaq.points) * 100.0 / ue.total_points) as percentage,
                RANK() OVER (PARTITION BY ea.exam_id ORDER BY SUM(eaq.points) DESC) as exam_rank,
                COUNT(*) OVER (PARTITION BY ea.exam_id) as totalStudents,
                ROW_NUMBER() OVER (PARTITION BY ea.exam_id ORDER BY ea.completed_at DESC) as rn
            FROM exam_attempt ea
            JOIN user_exams ue ON ea.exam_id = ue.exam_id
            JOIN exam_attempt_question eaq ON ea.attempt_id = eaq.attempt_id
            WHERE ea.user_id = :studentId
            AND ue.class_id = :classId
            AND ea.status = 'DONE'
            GROUP BY ea.exam_id, ea.attempt_id, ea.completed_at, ue.total_points
        )
        SELECT 
            AVG(percentage) as overallAverage,
            COUNT(DISTINCT exam_id) as totalExamsTaken,
            AVG(exam_rank) as averageRank,
            AVG((totalStudents - exam_rank + 1) * 100.0 / totalStudents) as averagePercentile,
            MAX(score) as highestScore,
            MIN(score) as lowestScore
        FROM student_scores
        WHERE rn = 1
        """, nativeQuery = true)
    StudentGradeSummaryProjection getStudentGradeSummary(@Param("studentId") Long studentId, @Param("classId") Long classId);
    
    // 학생 시험 이력
    @Query(value = """
        SELECT 
            ea.exam_id as examId,
            ue.exam_name as examName,
            ea.completed_at as examDate,
            SUM(eaq.points) as score,
            ue.total_points as totalPoints,
            (SUM(eaq.points) * 100.0 / ue.total_points) as percentage,
            RANK() OVER (PARTITION BY ea.exam_id ORDER BY SUM(eaq.points) DESC) as exam_rank,
            COUNT(*) OVER (PARTITION BY ea.exam_id) as totalStudents,
            PERCENT_RANK() OVER (PARTITION BY ea.exam_id ORDER BY SUM(eaq.points)) * 100 as percentile,
            TIMESTAMPDIFF(MINUTE, ea.started_at, ea.completed_at) as timeTaken
        FROM exam_attempt ea
        JOIN user_exams ue ON ea.exam_id = ue.exam_id
        JOIN exam_attempt_question eaq ON ea.attempt_id = eaq.attempt_id
        WHERE ea.user_id = :studentId
        AND ue.class_id = :classId
        AND ea.status = 'DONE'
        GROUP BY ea.exam_id, ue.exam_name, ea.attempt_id, ea.started_at, ea.completed_at, ue.total_points
        ORDER BY ea.completed_at DESC
        """, nativeQuery = true)
    List<StudentExamHistoryProjection> getStudentExamHistory(@Param("studentId") Long studentId, @Param("classId") Long classId);
    
    // 점수 구간별 분포
    @Query(value = """
        WITH score_ranges AS (
            SELECT 
                CASE 
                    WHEN score BETWEEN 0 AND 10 THEN '0-10'
                    WHEN score BETWEEN 11 AND 20 THEN '11-20'
                    WHEN score BETWEEN 21 AND 30 THEN '21-30'
                    WHEN score BETWEEN 31 AND 40 THEN '31-40'
                    WHEN score BETWEEN 41 AND 50 THEN '41-50'
                    WHEN score BETWEEN 51 AND 60 THEN '51-60'
                    WHEN score BETWEEN 61 AND 70 THEN '61-70'
                    WHEN score BETWEEN 71 AND 80 THEN '71-80'
                    WHEN score BETWEEN 81 AND 90 THEN '81-90'
                    WHEN score BETWEEN 91 AND 100 THEN '91-100'
                END as range,
                COUNT(*) as count
            FROM (
                SELECT (SUM(eaq.points) * 100.0 / ue.total_points) as score
                FROM exam_attempt ea
                JOIN user_exams ue ON ea.exam_id = ue.exam_id
                JOIN exam_attempt_question eaq ON ea.attempt_id = eaq.attempt_id
                JOIN students s ON ea.user_id = s.user_id
                WHERE s.class_group_id = :classId
                AND (:examId IS NULL OR ea.exam_id = :examId)
                AND ea.status = 'DONE'
                GROUP BY ea.attempt_id, ue.total_points
            ) scores
            GROUP BY range
        )
        SELECT 
            range,
            count,
            (count * 100.0 / SUM(count) OVER ()) as percentage
        FROM score_ranges
        ORDER BY range
        """, nativeQuery = true)
    List<ScoreRangeDistributionProjection> getScoreRangeDistribution(@Param("classId") Long classId, @Param("examId") Long examId);
    
    // 등급별 수 분포
    @Query(value = """
        WITH grade_counts AS (
            SELECT 
                SUM(CASE WHEN percentage >= 90 THEN 1 ELSE 0 END) as gradeA,
                SUM(CASE WHEN percentage >= 80 AND percentage < 90 THEN 1 ELSE 0 END) as gradeB,
                SUM(CASE WHEN percentage >= 70 AND percentage < 80 THEN 1 ELSE 0 END) as gradeC,
                SUM(CASE WHEN percentage >= 60 AND percentage < 70 THEN 1 ELSE 0 END) as gradeD,
                SUM(CASE WHEN percentage < 60 THEN 1 ELSE 0 END) as gradeF
            FROM (
                SELECT (SUM(eaq.points) * 100.0 / ue.total_points) as percentage
                FROM exam_attempt ea
                JOIN user_exams ue ON ea.exam_id = ue.exam_id
                JOIN exam_attempt_question eaq ON ea.attempt_id = eaq.attempt_id
                JOIN students s ON ea.user_id = s.user_id
                WHERE s.class_group_id = :classId
                AND (:examId IS NULL OR ea.exam_id = :examId)
                AND ea.status = 'DONE'
                GROUP BY ea.attempt_id, ue.total_points
            ) scores
        )
        SELECT * FROM grade_counts
        """, nativeQuery = true)
    GradeCountDistributionProjection getGradeCountDistribution(@Param("classId") Long classId, @Param("examId") Long examId);
    
    // 백분위 분포
    @Query(value = """
        WITH scores AS (
            SELECT (SUM(eaq.points) * 100.0 / ue.total_points) as score
            FROM exam_attempt ea
            JOIN user_exams ue ON ea.exam_id = ue.exam_id
            JOIN exam_attempt_question eaq ON ea.attempt_id = eaq.attempt_id
            JOIN students s ON ea.user_id = s.user_id
            WHERE s.class_group_id = :classId
            AND (:examId IS NULL OR ea.exam_id = :examId)
            AND ea.status = 'DONE'
            GROUP BY ea.attempt_id, ue.total_points
        )
        SELECT 
            MIN(score) as p10,
            MIN(score) as p25,
            AVG(score) as p50,
            MAX(score) as p75,
            MAX(score) as p90
        FROM scores
        """, nativeQuery = true)
    PercentileDistributionProjection getPercentileDistribution(@Param("classId") Long classId, @Param("examId") Long examId);
    
    // 박스플롯 데이터
    @Query(value = """
        WITH scores AS (
            SELECT (SUM(eaq.points) * 100.0 / ue.total_points) as score
            FROM exam_attempt ea
            JOIN user_exams ue ON ea.exam_id = ue.exam_id
            JOIN exam_attempt_question eaq ON ea.attempt_id = eaq.attempt_id
            JOIN students s ON ea.user_id = s.user_id
            WHERE s.class_group_id = :classId
            AND (:examId IS NULL OR ea.exam_id = :examId)
            AND ea.status = 'DONE'
            GROUP BY ea.attempt_id, ue.total_points
        ),
        quartiles AS (
            SELECT 
                MIN(score) as min,
                MIN(score) as q1,
                AVG(score) as median,
                MAX(score) as q3,
                MAX(score) as max,
                NULL as outliers
            FROM scores
        )
        SELECT * FROM quartiles
        """, nativeQuery = true)
    BoxPlotDataProjection getBoxPlotData(@Param("classId") Long classId, @Param("examId") Long examId);
    
    // 시험 비교 데이터
    @Query(value = """
        WITH exam_stats AS (
            SELECT 
                ue.exam_id as examId,
                ue.exam_name as examName,
                MAX(ea.completed_at) as examDate,
                ue.total_points as totalPoints,
                COUNT(DISTINCT ea.user_id) as participantCount,
                AVG(SUM(eaq.points)) as averageScore,
                AVG(SUM(eaq.points)) as medianScore,
                STDDEV(SUM(eaq.points)) as standardDeviation,
                (SUM(CASE WHEN SUM(eaq.points) >= ue.total_points * 0.6 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) as passRate
            FROM exam_attempt ea
            JOIN user_exams ue ON ea.exam_id = ue.exam_id
            JOIN exam_attempt_question eaq ON ea.attempt_id = eaq.attempt_id
            JOIN students s ON ea.user_id = s.user_id
            WHERE s.class_group_id = :classId
            AND ea.status = 'DONE'
            GROUP BY ue.exam_id, ue.exam_name, ea.attempt_id, ue.total_points
        )
        SELECT 
            *,
            LAG(averageScore) OVER (ORDER BY examDate) as previousAverage,
            AVG(averageScore) OVER () as overallAverage
        FROM exam_stats
        ORDER BY examDate DESC
        """, nativeQuery = true)
    List<ExamComparisonProjection> getExamComparisonData(@Param("classId") Long classId);
}

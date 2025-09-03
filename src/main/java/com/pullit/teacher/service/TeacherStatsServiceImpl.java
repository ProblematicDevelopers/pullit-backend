package com.pullit.teacher.service;

import com.pullit.classes.entity.Classes;
import com.pullit.classes.repository.ClassRepository;
import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;
import com.pullit.teacher.dto.response.*;
import com.pullit.teacher.projection.*;
import com.pullit.teacher.repository.TeacherStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherStatsServiceImpl implements TeacherStatsService {
    
    private final TeacherStatsRepository teacherStatsRepository;
    private final ClassRepository classRepository;
    
    @Override
    public ClassGradeOverviewResponse getClassGradeOverview(Long classId, Long teacherId) {
        // 교사 권한 확인
        validateTeacherAccess(teacherId, classId);
        
        // 클래스 정보 조회
        Classes classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLASS_NOT_FOUND));
        
        // 전체 학생 수 조회
        Integer totalStudents = teacherStatsRepository.countStudentsByClassId(classId);
        
        // 전체 시험 수 조회
        Integer totalExams = teacherStatsRepository.countExamsByClassId(classId);
        
        // 클래스 전체 통계 조회
        var classStats = teacherStatsRepository.getClassOverallStatistics(classId);
        
        // 최근 시험 요약 조회 (최근 5개)
        List<ClassGradeOverviewResponse.RecentExamSummary> recentExams = 
            teacherStatsRepository.getRecentExamSummaries(classId, 5).stream()
                .map(projection -> ClassGradeOverviewResponse.RecentExamSummary.builder()
                    .examId(projection.getExamId())
                    .examName(projection.getExamName())
                    .examDate(projection.getExamDate())
                    .averageScore(projection.getAverageScore())
                    .participantCount(projection.getParticipantCount())
                    .build())
                .collect(Collectors.toList());
        
        // 성적 분포 조회
        var gradeDistribution = teacherStatsRepository.getGradeRangeDistribution(classId);
        
        return ClassGradeOverviewResponse.builder()
                .classId(classId)
                .className(classEntity.getClassName())
                .totalStudents(totalStudents)
                .totalExams(totalExams)
                .classAverageScore(classStats != null && classStats.getAverageScore() != null ? classStats.getAverageScore() : 0.0)
                .classMedianScore(classStats != null && classStats.getMedianScore() != null ? classStats.getMedianScore() : 0.0)
                .highestScore(classStats != null && classStats.getHighestScore() != null ? classStats.getHighestScore() : 0.0)
                .lowestScore(classStats != null && classStats.getLowestScore() != null ? classStats.getLowestScore() : 0.0)
                .lastExamDate(classStats != null ? classStats.getLastExamDate() : null)
                .recentExams(recentExams)
                .gradeDistribution(ClassGradeOverviewResponse.GradeRangeCount.builder()
                    .excellent(gradeDistribution.getExcellent())
                    .good(gradeDistribution.getGood())
                    .average(gradeDistribution.getAverage())
                    .belowAverage(gradeDistribution.getBelowAverage())
                    .poor(gradeDistribution.getPoor())
                    .build())
                .build();
    }
    
    @Override
    public List<StudentGradeResponse> getAllStudentsGrades(Long classId, Long examId, Long teacherId) {
        // 교사 권한 확인
        validateTeacherAccess(teacherId, classId);
        
        // 학생별 성적 조회
        if (examId != null) {
            // 특정 시험에 대한 학생 성적
            var examInfo = teacherStatsRepository.getExamBasicInfo(examId);
            String examName = examInfo != null ? examInfo.getExamName() : null;
            return teacherStatsRepository.getStudentGradesByExam(classId, examId).stream()
                .map(p -> convertToStudentGradeResponse(p, examId, examName))
                .collect(Collectors.toList());
        } else {
            // 전체 시험에 대한 학생 성적
            try {
                return teacherStatsRepository.getAllStudentGrades(classId).stream()
                    .map(this::convertToStudentGradeResponse)
                    .collect(Collectors.toList());
            } catch (Exception e) {
                // 윈도우 함수 미지원 등 호환성 문제 시 기본 쿼리로 대체
                return teacherStatsRepository.getAllStudentGradesBasic(classId).stream()
                    .map(this::convertToStudentGradeResponse)
                    .collect(Collectors.toList());
            }
        }
    }
    
    @Override
    public ExamResultDetailResponse getExamResultDetail(Long classId, Long examId, Long teacherId) {
        // 교사 권한 확인
        validateTeacherAccess(teacherId, classId);
        
        // 시험 기본 정보 조회
        var examInfo = teacherStatsRepository.getExamBasicInfo(examId);
        if (examInfo == null) {
            throw new BusinessException(ErrorCode.EXAM_NOT_FOUND);
        }
        
        // 시험 통계 조회
        var statistics = teacherStatsRepository.getExamStatistics(classId, examId);
        
        // 학생별 결과 조회
        var studentResults = teacherStatsRepository.getStudentResultsByExam(classId, examId).stream()
            .map(this::convertToStudentResult)
            .collect(Collectors.toList());
        
        // 문제별 정답률 조회
        var questionStats = teacherStatsRepository.getQuestionStatistics(examId).stream()
            .map(this::convertToQuestionStatistics)
            .collect(Collectors.toList());
        
        return ExamResultDetailResponse.builder()
                .examId(examId)
                .examName(examInfo.getExamName())
                .examDate(examInfo.getExamDate())
                .totalPoints(examInfo.getTotalPoints() != null ? examInfo.getTotalPoints().intValue() : null)
                .totalStudents(studentResults.size())
                .participantCount(studentResults.size())
                .statistics(ExamResultDetailResponse.ExamStatistics.builder()
                    .averageScore(statistics.getAverageScore())
                    .medianScore(statistics.getMedianScore())
                    .standardDeviation(statistics.getStandardDeviation())
                    .highestScore(statistics.getHighestScore())
                    .lowestScore(statistics.getLowestScore())
                    .passRate(statistics.getPassRate())
                    .excellentRate(statistics.getExcellentRate())
                    .build())
                .studentResults(studentResults)
                .questionStats(questionStats)
                .build();
    }
    
    @Override
    public StudentDetailGradeResponse getStudentGradeDetail(Long classId, Long studentId, Long teacherId) {
        // 교사 권한 확인
        validateTeacherAccess(teacherId, classId);
        
        // 학생 기본 정보 조회
        var studentInfo = teacherStatsRepository.getStudentBasicInfo(studentId);
        if (studentInfo == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        // 성적 요약 조회
        var summary = teacherStatsRepository.getStudentGradeSummary(studentId, classId);
        
        // 시험 이력 조회
        var examHistory = teacherStatsRepository.getStudentExamHistory(studentId, classId).stream()
            .map(this::convertToExamDetail)
            .collect(Collectors.toList());
        
        // 성적 추이 분석
        var analysis = analyzeStudentPerformance(examHistory);
        
        return StudentDetailGradeResponse.builder()
                .studentId(studentId)
                .studentName(studentInfo.getStudentName())
                .studentNo(studentInfo.getStudentNo())
                .grade(studentInfo.getGrade())
                .schoolName(studentInfo.getSchoolName())
                .summary(StudentDetailGradeResponse.GradeSummary.builder()
                    .overallAverage(summary.getOverallAverage())
                    .totalExamsTaken(summary.getTotalExamsTaken())
                    .averageRank(summary.getAverageRank() != null ? summary.getAverageRank().intValue() : null)
                    .averagePercentile(summary.getAveragePercentile())
                    .overallGrade(calculateGrade(summary.getOverallAverage()))
                    .highestScore(summary.getHighestScore() != null ? summary.getHighestScore().intValue() : null)
                    .lowestScore(summary.getLowestScore() != null ? summary.getLowestScore().intValue() : null)
                    .build())
                .examHistory(examHistory)
                .analysis(analysis)
                .build();
    }
    
    @Override
    public GradeDistributionResponse getGradeDistribution(Long classId, Long examId, Long teacherId) {
        // 교사 권한 확인
        validateTeacherAccess(teacherId, classId);
        
        // 클래스 정보 조회
        Classes classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLASS_NOT_FOUND));
        
        // 점수 구간별 분포 조회
        var scoreDistribution = teacherStatsRepository.getScoreRangeDistribution(classId, examId).stream()
            .map(proj -> GradeDistributionResponse.ScoreRange.builder()
                .range(proj.getRange())
                .count(proj.getCount())
                .percentage(proj.getPercentage())
                .build())
            .collect(Collectors.toList());
        
        // 등급별 분포 조회
        var gradeCount = teacherStatsRepository.getGradeCountDistribution(classId, examId);
        
        // 백분위 분포 조회
        var percentiles = teacherStatsRepository.getPercentileDistribution(classId, examId);
        
        // 박스플롯 데이터 조회
        var boxPlot = teacherStatsRepository.getBoxPlotData(classId, examId);
        
        return GradeDistributionResponse.builder()
                .classId(classId)
                .className(classEntity.getClassName())
                .examId(examId)
                .scoreDistribution(scoreDistribution)
                .gradeCount(GradeDistributionResponse.GradeCount.builder()
                    .gradeA(gradeCount.getGradeA())
                    .gradeB(gradeCount.getGradeB())
                    .gradeC(gradeCount.getGradeC())
                    .gradeD(gradeCount.getGradeD())
                    .gradeF(gradeCount.getGradeF())
                    .build())
                .percentiles(GradeDistributionResponse.PercentileDistribution.builder()
                    .p10(percentiles.getP10())
                    .p25(percentiles.getP25())
                    .p50(percentiles.getP50())
                    .p75(percentiles.getP75())
                    .p90(percentiles.getP90())
                    .build())
                .boxPlot(GradeDistributionResponse.BoxPlotData.builder()
                    .min(boxPlot.getMin())
                    .q1(boxPlot.getQ1())
                    .median(boxPlot.getMedian())
                    .q3(boxPlot.getQ3())
                    .max(boxPlot.getMax())
                    .outliers(List.of()) // 현재는 빈 리스트로 처리
                    .build())
                .build();
    }
    
    @Override
    public List<ExamComparisonResponse> getExamComparison(Long classId, Long teacherId) {
        // 교사 권한 확인
        validateTeacherAccess(teacherId, classId);
        
        // 시험별 비교 데이터 조회
        return teacherStatsRepository.getExamComparisonData(classId).stream()
            .map(this::convertToExamComparisonResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    public boolean isTeacherOfClass(Long teacherId, Long classId) {
        Long cnt = teacherStatsRepository.countTeacherOfClass(teacherId, classId);
        return cnt != null && cnt > 0;
    }
    
    // Private helper methods
    private void validateTeacherAccess(Long teacherId, Long classId) {
        if (!isTeacherOfClass(teacherId, classId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }
    
    private StudentGradeResponse convertToStudentGradeResponse(StudentGradeProjection projection) {
        return StudentGradeResponse.builder()
            .studentId(projection.getStudentId())
            .studentName(projection.getStudentName())
            .studentNo(projection.getStudentNo())
            .averageScore(projection.getAverageScore())
            .totalExamsTaken(projection.getTotalExamsTaken())
            .percentile(projection.getPercentile())
            .classRank(projection.getClassRank())
            .trend(projection.getTrend())
            .build();
    }

    private StudentGradeResponse convertToStudentGradeResponse(
            StudentGradeByExamProjection projection,
            Long examId,
            String examName) {
        // 백분위 추정 (rank와 전체 인원 기반)
        Double percentile = null;
        if (projection.getTotalStudents() != null && projection.getTotalStudents() > 0 && projection.getExamRank() != null) {
            percentile = (projection.getTotalStudents() - projection.getExamRank() + 1) * 100.0 / projection.getTotalStudents();
        }

        StudentGradeResponse.ExamScore examScore = StudentGradeResponse.ExamScore.builder()
                .examId(examId)
                .examName(examName)
                .score(projection.getScore() != null ? projection.getScore().intValue() : null)
                .totalPoints(projection.getTotalPoints() != null ? projection.getTotalPoints().intValue() : null)
                .percentage(projection.getPercentage())
                .rank(projection.getExamRank())
                .grade(projection.getGrade())
                .build();

        return StudentGradeResponse.builder()
                .studentId(projection.getStudentId())
                .studentName(projection.getStudentName())
                .studentNo(projection.getStudentNo())
                // 단일 시험 기준으로 평균 점수는 해당 시험 퍼센티지로 설정
                .averageScore(projection.getPercentage())
                .totalExamsTaken(1)
                .classRank(projection.getExamRank())
                .percentile(percentile)
                .examScores(List.of(examScore))
                .trend("STABLE")
                .build();
    }
    
    private ExamResultDetailResponse.StudentResult convertToStudentResult(StudentResultProjection projection) {
        return ExamResultDetailResponse.StudentResult.builder()
            .studentId(projection.getStudentId())
            .studentName(projection.getStudentName())
            .studentNo(projection.getStudentNo())
            .score(projection.getScore() != null ? projection.getScore().intValue() : null)
            .percentage(projection.getPercentage())
            .rank(projection.getExamRank())
            .grade(projection.getGrade())
            .completedAt(projection.getCompletedAt())
            .timeTaken(projection.getTimeTaken())
            .build();
    }
    
    private ExamResultDetailResponse.QuestionStatistics convertToQuestionStatistics(QuestionStatisticsProjection projection) {
        return ExamResultDetailResponse.QuestionStatistics.builder()
            .questionNumber(projection.getQuestionNumber())
            .questionType(projection.getQuestionType())
            .correctCount(projection.getCorrectCount())
            .correctRate(projection.getCorrectRate())
            .points(projection.getPoints() != null ? projection.getPoints().intValue() : null)
            .build();
    }
    
    private StudentDetailGradeResponse.ExamDetail convertToExamDetail(StudentExamHistoryProjection projection) {
        return StudentDetailGradeResponse.ExamDetail.builder()
            .examId(projection.getExamId())
            .examName(projection.getExamName())
            .examDate(projection.getExamDate())
            .score(projection.getScore() != null ? projection.getScore().intValue() : null)
            .totalPoints(projection.getTotalPoints() != null ? projection.getTotalPoints().intValue() : null)
            .percentage(projection.getPercentage())
            .rank(projection.getExamRank())
            .totalStudents(projection.getTotalStudents())
            .percentile(projection.getPercentile())
            .timeTaken(projection.getTimeTaken())
            .build();
    }
    
    private ExamComparisonResponse convertToExamComparisonResponse(ExamComparisonProjection projection) {
        Double currentAvg = projection.getAverageScore();
        Double prevAvg = projection.getPreviousAverage();
        Double overallAvg = projection.getOverallAverage();

        ExamComparisonResponse.ComparisonMetrics prevMetrics = null;
        if (currentAvg != null && prevAvg != null) {
            double diff = currentAvg - prevAvg;
            Double pct = (prevAvg != 0.0) ? (diff / prevAvg) * 100.0 : null;
            String trend = diff > 0 ? "UP" : (diff < 0 ? "DOWN" : "SAME");
            prevMetrics = ExamComparisonResponse.ComparisonMetrics.builder()
                    .scoreDifference(diff)
                    .percentageChange(pct)
                    .trend(trend)
                    .isImproved(diff > 0)
                    .build();
        }

        ExamComparisonResponse.ComparisonMetrics overallMetrics = null;
        if (currentAvg != null && overallAvg != null) {
            double diff = currentAvg - overallAvg;
            Double pct = (overallAvg != 0.0) ? (diff / overallAvg) * 100.0 : null;
            String trend = diff > 0 ? "UP" : (diff < 0 ? "DOWN" : "SAME");
            overallMetrics = ExamComparisonResponse.ComparisonMetrics.builder()
                    .scoreDifference(diff)
                    .percentageChange(pct)
                    .trend(trend)
                    .isImproved(diff > 0)
                    .build();
        }

        return ExamComparisonResponse.builder()
                .examId(projection.getExamId())
                .examName(projection.getExamName())
                .examDate(projection.getExamDate())
                .totalPoints(projection.getTotalPoints() != null ? projection.getTotalPoints().intValue() : null)
                .participantCount(projection.getParticipantCount())
                .averageScore(currentAvg)
                .medianScore(projection.getMedianScore())
                .standardDeviation(projection.getStandardDeviation())
                .passRate(projection.getPassRate())
                .comparisonToPrevious(prevMetrics)
                .comparisonToOverall(overallMetrics)
                .build();
    }
    
    private StudentDetailGradeResponse.PerformanceAnalysis analyzeStudentPerformance(
            List<StudentDetailGradeResponse.ExamDetail> examHistory) {
        // 성적 추이 분석 로직
        String trend = "STABLE";
        double trendScore = 0.0;
        
        if (examHistory.size() >= 2) {
            // 최근 성적과 이전 성적 비교
            double recentAvg = examHistory.stream()
                .limit(3)
                .mapToDouble(e -> e.getPercentage())
                .average()
                .orElse(0.0);
            
            double previousAvg = examHistory.stream()
                .skip(Math.max(0, examHistory.size() - 3))
                .mapToDouble(e -> e.getPercentage())
                .average()
                .orElse(0.0);
            
            trendScore = recentAvg - previousAvg;
            
            if (trendScore > 5) {
                trend = "IMPROVING";
            } else if (trendScore < -5) {
                trend = "DECLINING";
            }
        }
        
        return StudentDetailGradeResponse.PerformanceAnalysis.builder()
            .trend(trend)
            .trendScore(trendScore)
            .strengths(List.of("문제 해결 능력", "시간 관리"))
            .weaknesses(List.of("응용 문제", "서술형 답안"))
            .recommendation("기초 개념 복습 후 응용 문제 연습 권장")
            .build();
    }
    
    private String calculateGrade(Double score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }
}

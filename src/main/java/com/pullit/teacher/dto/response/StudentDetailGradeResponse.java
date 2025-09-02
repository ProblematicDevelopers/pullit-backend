package com.pullit.teacher.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDetailGradeResponse {
    
    private Long studentId;
    private String studentName;
    private String studentNo;
    private String grade;
    private String schoolName;
    
    // 전체 성적 요약
    private GradeSummary summary;
    
    // 시험별 상세 성적
    private List<ExamDetail> examHistory;
    
    // 성적 추이 분석
    private PerformanceAnalysis analysis;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GradeSummary {
        private Double overallAverage;
        private Integer totalExamsTaken;
        private Integer averageRank;
        private Double averagePercentile;
        private String overallGrade;
        private Integer highestScore;
        private Integer lowestScore;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExamDetail {
        private Long examId;
        private String examName;
        private LocalDateTime examDate;
        private Integer score;
        private Integer totalPoints;
        private Double percentage;
        private Integer rank;
        private Integer totalStudents;
        private Double percentile;
        private String grade;
        private Integer timeTaken;
        private List<QuestionResult> questionResults;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionResult {
        private Integer questionNumber;
        private Boolean isCorrect;
        private Integer points;
        private String userAnswer;
        private String correctAnswer;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceAnalysis {
        private String trend; // IMPROVING, DECLINING, STABLE
        private Double trendScore; // -100 to +100
        private List<String> strengths;
        private List<String> weaknesses;
        private String recommendation;
    }
}
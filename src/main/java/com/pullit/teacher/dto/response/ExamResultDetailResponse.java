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
public class ExamResultDetailResponse {
    
    private Long examId;
    private String examName;
    private LocalDateTime examDate;
    private Integer totalPoints;
    private Integer totalStudents;
    private Integer participantCount;
    
    // 통계 정보
    private ExamStatistics statistics;
    
    // 학생별 결과
    private List<StudentResult> studentResults;
    
    // 문제별 정답률
    private List<QuestionStatistics> questionStats;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExamStatistics {
        private Double averageScore;
        private Double medianScore;
        private Double standardDeviation;
        private Double highestScore;
        private Double lowestScore;
        private Double passRate; // 60점 이상 비율
        private Double excellentRate; // 90점 이상 비율
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentResult {
        private Long studentId;
        private String studentName;
        private String studentNo;
        private Integer score;
        private Double percentage;
        private Integer rank;
        private String grade;
        private LocalDateTime completedAt;
        private Integer timeTaken; // 분 단위
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionStatistics {
        private Integer questionNumber;
        private String questionType;
        private Integer correctCount;
        private Double correctRate;
        private Integer points;
    }
}
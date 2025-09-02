package com.pullit.teacher.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentGradeResponse {
    
    private Long studentId;
    private String studentName;
    private String studentNo;
    private Double averageScore;
    private Integer totalExamsTaken;
    private Integer classRank;
    private Double percentile;
    
    // 각 시험별 성적
    private List<ExamScore> examScores;
    
    // 성적 추이
    private String trend; // IMPROVING, DECLINING, STABLE
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExamScore {
        private Long examId;
        private String examName;
        private Integer score;
        private Integer totalPoints;
        private Double percentage;
        private Integer rank;
        private String grade; // A, B, C, D, F
    }
}
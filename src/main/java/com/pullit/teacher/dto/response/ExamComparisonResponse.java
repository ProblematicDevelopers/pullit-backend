package com.pullit.teacher.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamComparisonResponse {
    
    private Long examId;
    private String examName;
    private LocalDateTime examDate;
    private Integer totalPoints;
    private Integer participantCount;
    
    // 통계 비교
    private Double averageScore;
    private Double medianScore;
    private Double standardDeviation;
    private Double passRate;
    
    // 이전 시험 대비
    private ComparisonMetrics comparisonToPrevious;
    
    // 전체 평균 대비
    private ComparisonMetrics comparisonToOverall;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparisonMetrics {
        private Double scoreDifference;
        private Double percentageChange;
        private String trend; // UP, DOWN, SAME
        private Boolean isImproved;
    }
}
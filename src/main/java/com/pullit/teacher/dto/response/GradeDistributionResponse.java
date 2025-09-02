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
public class GradeDistributionResponse {
    
    private Long classId;
    private String className;
    private Long examId;
    private String examName;
    
    // 점수 구간별 분포
    private List<ScoreRange> scoreDistribution;
    
    // 등급별 분포
    private GradeCount gradeCount;
    
    // 백분위 분포
    private PercentileDistribution percentiles;
    
    // 박스플롯 데이터
    private BoxPlotData boxPlot;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreRange {
        private String range; // "0-10", "11-20", etc.
        private Integer count;
        private Double percentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GradeCount {
        private Integer gradeA; // 90-100
        private Integer gradeB; // 80-89
        private Integer gradeC; // 70-79
        private Integer gradeD; // 60-69
        private Integer gradeF; // 0-59
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PercentileDistribution {
        private Double p10; // 10th percentile
        private Double p25; // 25th percentile (Q1)
        private Double p50; // 50th percentile (Median)
        private Double p75; // 75th percentile (Q3)
        private Double p90; // 90th percentile
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BoxPlotData {
        private Double min;
        private Double q1;
        private Double median;
        private Double q3;
        private Double max;
        private List<Double> outliers;
    }
}
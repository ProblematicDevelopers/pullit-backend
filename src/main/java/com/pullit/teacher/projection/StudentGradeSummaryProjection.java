package com.pullit.teacher.projection;

public interface StudentGradeSummaryProjection {
    Double getOverallAverage();
    Integer getTotalExamsTaken();
    Double getAverageRank();
    Double getAveragePercentile();
    Double getHighestScore();
    Double getLowestScore();
}
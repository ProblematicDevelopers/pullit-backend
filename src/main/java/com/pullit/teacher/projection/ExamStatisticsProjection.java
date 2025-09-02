package com.pullit.teacher.projection;

public interface ExamStatisticsProjection {
    Double getAverageScore();
    Double getMedianScore();
    Double getStandardDeviation();
    Double getHighestScore();
    Double getLowestScore();
    Double getPassRate();
    Double getExcellentRate();
}
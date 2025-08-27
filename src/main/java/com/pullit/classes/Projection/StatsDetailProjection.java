package com.pullit.classes.Projection;

public interface StatsDetailProjection {
    Long getExamId();
    String getExamName();
    Long getScore();
    Long getRankPosition();
    Long getTotalStudent();
    Long getPercentile();
    Long getQuartile();
    String getQuartileDescription();
    Double getTopPercentage();
}

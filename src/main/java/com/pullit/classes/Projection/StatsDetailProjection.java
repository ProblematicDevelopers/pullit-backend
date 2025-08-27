package com.pullit.classes.Projection;

public interface StatsDetailProjection {
    Long getExamId();
    String getExamName();
    Long getScore();
    Long getRankPosition();
    Long getTotalStudents();
    Long getPercentile();
    Long getQuartile();
    String getQuartileDescription();
    Double getTopPercentage();
}

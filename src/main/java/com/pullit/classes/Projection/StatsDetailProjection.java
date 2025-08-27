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
    Double getMedian();
    Double getMean();
    Long getMax();
    Long getMin();
    Double getStdDeviation();
    Double getQ1();
    Double getQ3();

}

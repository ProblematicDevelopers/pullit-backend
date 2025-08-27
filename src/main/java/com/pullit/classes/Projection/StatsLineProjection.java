package com.pullit.classes.Projection;

public interface StatsLineProjection {
    Long getExamId();
    String getExamName();
    Long getTotalPoints();
    Long getUserPoints();
    Double getAvgPoints();
    Long getMaxPoints();
    Long getMinPoints();
}

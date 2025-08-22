package com.pullit.cbt.projection;

public interface DetailEvaluationProjection {
    String getDomainName();
    Long getTotalCount();
    Long getUserCount();
    Double getAvgCount();
    Long getTotalPoints();
    Long getUserPoints();
    Double getAvgPoints();
    Double getUserDuration();
    Double getAvgDuration();
}

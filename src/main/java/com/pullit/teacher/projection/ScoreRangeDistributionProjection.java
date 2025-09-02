package com.pullit.teacher.projection;

public interface ScoreRangeDistributionProjection {
    String getRange();
    Integer getCount();
    Double getPercentage();
}
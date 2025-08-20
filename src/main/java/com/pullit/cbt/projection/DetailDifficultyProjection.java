package com.pullit.cbt.projection;

public interface DetailDifficultyProjection {
    String getDifficultyCode();
    Long getItemCount();
    Long getTotalPoints();
    Long getUserPoints();
    Double getAvgPoints();
    Double getAvgCount();
    Long getUserCount();
    Double getUserDuration();
    Double getAvgDuration();
}

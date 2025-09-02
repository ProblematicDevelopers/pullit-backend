package com.pullit.teacher.projection;

public interface GradeRangeDistributionProjection {
    Integer getExcellent();
    Integer getGood();
    Integer getAverage();
    Integer getBelowAverage();
    Integer getPoor();
}
package com.pullit.teacher.projection;

import java.time.LocalDateTime;

public interface ClassOverallStatisticsProjection {
    Double getAverageScore();
    Double getMedianScore();
    Double getHighestScore();
    Double getLowestScore();
    LocalDateTime getLastExamDate();
}
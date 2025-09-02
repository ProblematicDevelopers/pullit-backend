package com.pullit.teacher.projection;

import java.time.LocalDateTime;

public interface ExamComparisonProjection {
    Long getExamId();
    String getExamName();
    LocalDateTime getExamDate();
    Double getTotalPoints();
    Integer getParticipantCount();
    Double getAverageScore();
    Double getMedianScore();
    Double getStandardDeviation();
    Double getPassRate();
    Double getPreviousAverage();
    Double getOverallAverage();
}
package com.pullit.teacher.projection;

import java.time.LocalDateTime;

public interface RecentExamSummaryProjection {
    Long getExamId();
    String getExamName();
    LocalDateTime getExamDate();
    Double getAverageScore();
    Integer getParticipantCount();
}
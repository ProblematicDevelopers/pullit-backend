package com.pullit.teacher.projection;

import java.time.LocalDateTime;

public interface StudentExamHistoryProjection {
    Long getExamId();
    String getExamName();
    LocalDateTime getExamDate();
    Double getScore();
    Double getTotalPoints();
    Double getPercentage();
    Integer getRank();
    Integer getTotalStudents();
    Double getPercentile();
    Integer getTimeTaken();
}
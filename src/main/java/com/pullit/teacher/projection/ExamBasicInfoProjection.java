package com.pullit.teacher.projection;

import java.time.LocalDateTime;

public interface ExamBasicInfoProjection {
    Long getExamId();
    String getExamName();
    LocalDateTime getExamDate();
    Double getTotalPoints();
}
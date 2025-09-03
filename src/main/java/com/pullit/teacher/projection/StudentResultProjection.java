package com.pullit.teacher.projection;

import java.time.LocalDateTime;

public interface StudentResultProjection {
    Long getStudentId();
    String getStudentName();
    String getStudentNo();
    Double getScore();
    Double getPercentage();
    Integer getExamRank();
    String getGrade();
    LocalDateTime getCompletedAt();
    Integer getTimeTaken();
}
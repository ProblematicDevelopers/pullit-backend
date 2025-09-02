package com.pullit.teacher.projection;

import java.time.LocalDateTime;

public interface StudentGradeByExamProjection {
    Long getStudentId();
    String getStudentName();
    String getStudentNo();
    Double getScore();
    Double getTotalPoints();
    Double getPercentage();
    Integer getRank();
    Integer getTotalStudents();
    String getGrade();
    LocalDateTime getCompletedAt();
}
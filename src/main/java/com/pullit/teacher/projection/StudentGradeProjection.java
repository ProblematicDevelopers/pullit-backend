package com.pullit.teacher.projection;

public interface StudentGradeProjection {
    Long getStudentId();
    String getStudentName();
    String getStudentNo();
    Double getAverageScore();
    Integer getTotalExamsTaken();
    Double getAvgRank();
    Double getPercentile();
    Integer getClassRank();
    String getTrend();
}
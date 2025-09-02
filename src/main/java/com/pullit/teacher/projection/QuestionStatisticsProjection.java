package com.pullit.teacher.projection;

public interface QuestionStatisticsProjection {
    Integer getQuestionNumber();
    String getQuestionType();
    Integer getCorrectCount();
    Double getCorrectRate();
    Double getPoints();
}
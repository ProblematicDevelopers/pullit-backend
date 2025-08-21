package com.pullit.cbt.projection;

public interface DetailErrataProjection {
    Long getUserExamId();
    Long getUserId();
    Long getItemId();
    Long getQuestionId();
    Long getDuration();
    String getDomainName();
    Integer getItemOrder();
    Integer getPoints();
    String getAnswer();
    String getUserAnswer();
    Boolean getIsCorrect();
    Integer getUserPoints();
    Double getAccuracy();
}

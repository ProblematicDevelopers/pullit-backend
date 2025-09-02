package com.pullit.teacher.projection;

public interface BoxPlotDataProjection {
    Double getMin();
    Double getQ1();
    Double getMedian();
    Double getQ3();
    Double getMax();
    String getOutliers(); // NULL로 반환되므로 String으로 처리
}
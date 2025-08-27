package com.pullit.classes.dto.response;

import com.pullit.classes.Projection.StatsDetailProjection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StatsDetailResponse {
    private Long examId;
    private String examName;
    private Long score;
    private Long rankPosition;
    private Long totalStudents;
    private Long percentile;
    private Long quartile;
    private String quartileDescription;
    private Double topPercentage;

    public static StatsDetailResponse from(StatsDetailProjection statsDetailProjection) {
        return StatsDetailResponse.builder()
                .examId(statsDetailProjection.getExamId())
                .examName(statsDetailProjection.getExamName())
                .score(statsDetailProjection.getScore())
                .rankPosition(statsDetailProjection.getRankPosition())
                .totalStudents(statsDetailProjection.getTotalStudents())
                .percentile(statsDetailProjection.getPercentile())
                .quartile(statsDetailProjection.getQuartile())
                .quartileDescription(statsDetailProjection.getQuartileDescription())
                .topPercentage(statsDetailProjection.getTopPercentage())
                .build();
    }
}

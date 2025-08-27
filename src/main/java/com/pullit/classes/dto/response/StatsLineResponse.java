package com.pullit.classes.dto.response;

import com.pullit.classes.Projection.StatsLineProjection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatsLineResponse {
    private Long examId;
    private String examName;
    private Long totalPoints;
    private Long userPoints;
    private Double avgPoints;
    private Long maxPoints;
    private Long minPoints;

    public static StatsLineResponse from(StatsLineProjection statsLineProjection) {
        return StatsLineResponse.builder()
                .examId(statsLineProjection.getExamId())
                .examName(statsLineProjection.getExamName())
                .totalPoints(statsLineProjection.getTotalPoints())
                .userPoints(statsLineProjection.getUserPoints())
                .avgPoints(statsLineProjection.getAvgPoints())
                .maxPoints(statsLineProjection.getMaxPoints())
                .minPoints(statsLineProjection.getMinPoints())
                .build();
    }
}

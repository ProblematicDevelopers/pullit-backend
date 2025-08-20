package com.pullit.cbt.dto.response;

import com.pullit.cbt.projection.DetailDifficultyProjection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DetailDifficultyResponse {
    private String difficultyCode;
    private Long itemCount;
    private Long totalPoints;
    private Long userPoints;
    private Double avgPoints;
    private Long userCount;
    private Double avgCount;
    private Double userDuration;
    private Double avgDuration;

    public static DetailDifficultyResponse from(DetailDifficultyProjection projection) {
        return DetailDifficultyResponse.builder()
                .difficultyCode(projection.getDifficultyCode())
                .itemCount(projection.getItemCount())
                .totalPoints(projection.getTotalPoints())
                .userPoints(projection.getUserPoints())
                .avgPoints(projection.getAvgPoints())
                .avgCount(projection.getAvgCount())
                .userCount(projection.getUserCount())
                .userDuration(projection.getUserDuration())
                .avgDuration(projection.getAvgDuration())
                .build();
    }
}

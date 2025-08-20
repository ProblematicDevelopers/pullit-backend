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
    private Double totalAvg;
    private Double userAvg;

    public static DetailDifficultyResponse from(DetailDifficultyProjection projection) {
        return DetailDifficultyResponse.builder()
                .difficultyCode(projection.getDifficultyCode())
                .itemCount(projection.getItemCount())
                .totalAvg(projection.getTotalAvg())
                .userAvg(projection.getUserAvg())
                .build();
    }
}

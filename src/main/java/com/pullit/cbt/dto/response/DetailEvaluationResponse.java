package com.pullit.cbt.dto.response;

import com.pullit.cbt.projection.DetailEvaluationProjection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DetailEvaluationResponse {
    private String domainName;
    private Long totalCount;
    private Long userCount;
    private Double avgCount;
    private Long totalPoints;
    private Long userPoints;
    private Double avgPoints;
    private Double userDuration;
    private Double avgDuration;

    public static DetailEvaluationResponse from(DetailEvaluationProjection projection) {
        return DetailEvaluationResponse.builder()
                .domainName(projection.getDomainName())
                .totalCount(projection.getTotalCount())
                .userCount(projection.getUserCount())
                .avgCount(projection.getAvgCount())
                .totalPoints(projection.getTotalPoints())
                .userPoints(projection.getUserPoints())
                .avgPoints(projection.getAvgPoints())
                .userDuration(projection.getUserDuration())
                .avgDuration(projection.getAvgDuration())
                .build();
    }
}

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

    public static DetailEvaluationResponse from(DetailEvaluationProjection projection) {
        return DetailEvaluationResponse.builder()
                .build();
    }
}

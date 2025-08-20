package com.pullit.cbt.dto.response;

import com.pullit.cbt.projection.DetailErrataProjection;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DetailErrataResponse {
    private Long userExamId;
    private Long userId;
    private Long itemId;
    private String domainName;
    private Integer itemOrder;
    private Integer points;
    private String answer;
    private String userAnswer;
    private Boolean isCorrect;
    private Integer userPoints;
    private Double accuracy;

    public static DetailErrataResponse from(DetailErrataProjection projection) {
        return DetailErrataResponse.builder()
                .userExamId(projection.getUserExamId())
                .userId(projection.getUserId())
                .itemId(projection.getItemId())
                .domainName(projection.getDomainName())
                .itemOrder(projection.getItemOrder())
                .points(projection.getPoints())
                .answer(projection.getAnswer())
                .userAnswer(projection.getUserAnswer())
                .isCorrect(projection.getIsCorrect())
                .userPoints(projection.getUserPoints())
                .accuracy(projection.getAccuracy())
                .build();
    }
}

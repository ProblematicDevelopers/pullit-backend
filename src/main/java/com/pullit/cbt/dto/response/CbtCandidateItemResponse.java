package com.pullit.cbt.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CbtCandidateItemResponse {

    private Long itemId;
    private Long subjectId;
    private Long difficultyCode;

    public CbtCandidateItemResponse(Long itemId, Long subjectId) {
        this.itemId = itemId;
        this.subjectId = subjectId;
    }
}

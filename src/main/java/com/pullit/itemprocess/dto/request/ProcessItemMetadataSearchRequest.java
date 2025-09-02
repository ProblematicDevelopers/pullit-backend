package com.pullit.itemprocess.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessItemMetadataSearchRequest {
    private Long subjectId;
    private List<Long> chapterIds;
    private Long difficultyCode;
    private Long questionFormCode;
    private Long passageId;
    private Boolean hasPassage; // true: 지문 있는 문항, false: 독립 문항, null: 전체
    private Boolean hasHtmlData;
    private Boolean hasImageData;
    private String keyword; // 검색 키워드
    private String sortBy; // 정렬 기준 (createdDate, itemId, difficulty 등)
    private String sortDirection; // ASC, DESC
    private Integer page;
    private Integer size;
}

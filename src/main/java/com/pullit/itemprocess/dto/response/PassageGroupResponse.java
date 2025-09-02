package com.pullit.itemprocess.dto.response;

import java.util.List;

import com.pullit.itemprocess.entity.ProcessItemMetadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassageGroupResponse {
    private Long passageId;
    private Long subjectId;
    private List<ProcessItemMetadata> items;
    private int itemCount;
    private String representativeDifficulty;
    
    // 지문 내용 (첫 번째 문항의 passage에서 추출)
    private String passageContent;
    private String passageHtml;
    
    // 지문 그룹 통계
    private Long minItemId;
    private Long maxItemId;
    private String createdAt;
    private String updatedAt;
}

package com.pullit.itemprocess.dto.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePassageGroupRequest {
    private Long subjectId;
    private List<Long> itemIds; // 지문 그룹에 포함할 문항 ID들
    private String passageContent; // 지문 내용 (선택사항)
    private String passageHtml; // 지문 HTML (선택사항)
}

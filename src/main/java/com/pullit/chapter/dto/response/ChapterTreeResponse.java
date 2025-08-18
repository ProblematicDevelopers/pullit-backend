package com.pullit.chapter.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChapterTreeResponse {

    private Long id;
    private String number;
    private String name;
    private Integer depth;
    private Integer itemCount;

    @Builder.Default
    private List<ChapterTreeResponse> children = new ArrayList<>();

    // UI 상태
    @Builder.Default
    private Boolean expanded = false;

    @Builder.Default
    private Boolean selected = false;

    // 프론트엔드 호환용
    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }
}

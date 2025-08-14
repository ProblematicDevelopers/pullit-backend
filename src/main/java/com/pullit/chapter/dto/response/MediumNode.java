package com.pullit.chapter.dto.response;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediumNode {
    private Long id;
    private String name;
    @Builder.Default
    private List<SmallNode> children = new ArrayList<>();
}

package com.pullit.chapter.dto.response;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LargeNode {
    private Long id;
    private String name;
    @Builder.Default
    private List<MediumNode> children = new ArrayList<>();
}

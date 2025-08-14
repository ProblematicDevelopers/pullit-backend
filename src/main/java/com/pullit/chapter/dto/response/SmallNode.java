package com.pullit.chapter.dto.response;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmallNode {
    private Long id;
    private String name;
    @Builder.Default
    private List<TopicNode> topics = new ArrayList<>();
}

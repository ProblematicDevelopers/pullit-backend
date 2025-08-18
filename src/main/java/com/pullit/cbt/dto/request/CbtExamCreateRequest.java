package com.pullit.cbt.dto.request;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CbtExamCreateRequest {
    private int questionCount;
    private Long subjectId;
    private List<SelectedChapter> selectedChapters;
    private int timeLimit;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SelectedChapter {
        private String largeChapterId;
        private List<Long> mediumChapters;
    }
}

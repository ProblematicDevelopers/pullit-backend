package com.pullit.item.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ItemSearchRequest {

    private Long subjectId;  // 선택적 - 교과서 필터링

    @Builder.Default
    private List<Long> largeChapterIds = new ArrayList<>();

    @Builder.Default
    private List<Long> mediumChapterIds = new ArrayList<>();

    @Builder.Default
    private List<Long> smallChapterIds = new ArrayList<>();

    @Builder.Default
    private List<Long> topicChapterIds = new ArrayList<>();

    @Builder.Default
    private List<Long> questionFormCode = new ArrayList<>();

    @Builder.Default
    private List<Long> difficultyCode = new ArrayList<>();

    private String keyword;

    @Min(0)
    @Builder.Default
    private Integer page = 0;

    @Min(1)
    @Max(100)
    @Builder.Default
    private Integer size = 20;

    @Builder.Default
    private String sortBy = "itemId";  // itemId, difficultyCode, largeChapterId

    @Builder.Default
    private String sortOrder = "ASC";  // ASC, DESC

    private Boolean hasImage;  // 이미지 포함 여부 필터

    private Boolean hasHtml;  // HTML 포함 여부 필터

    private Long passageId;  // 지문 ID로 필터링

    @Builder.Default
    private String itemSource = "REGULAR";  // REGULAR, OCR, BOTH - 문항 소스 선택

    // 편의 메서드
    public String getSort() {
        return sortBy + "," + sortOrder;
    }

    public boolean hasDifficultyFilter() {
        return difficultyCode != null && !difficultyCode.isEmpty();
    }

    public boolean hasQuestionFormFilter() {
        return questionFormCode != null && !questionFormCode.isEmpty();
    }

    public boolean hasChapterFilter() {
        return (largeChapterIds != null && !largeChapterIds.isEmpty()) ||
                (mediumChapterIds != null && !mediumChapterIds.isEmpty()) ||
                (smallChapterIds != null && !smallChapterIds.isEmpty()) ||
                (topicChapterIds != null && !topicChapterIds.isEmpty());
    }

    public boolean hasKeyword() {
        return keyword != null && !keyword.trim().isEmpty();
    }
}

package com.pullit.item.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartSelectionRequest {

    @NotNull(message = "교과서 ID는 필수입니다")
    private Long subjectId;

    @NotEmpty(message = "챕터를 선택해주세요")
    private List<Long> chapters;

    @Min(1)
    @Max(100)
    @NotNull(message = "문항 수는 필수입니다")
    private Integer itemCount;

    @NotBlank(message = "난이도를 선택해주세요")
    private String difficulty; // easy, normal, hard, mixed

    private List<Long> questionTypes;

    @Builder.Default
    private boolean includePassage = true; // 지문 포함 여부

    @Builder.Default
    private boolean avoidDuplicate = true;

    @Builder.Default
    private boolean prioritizeLatest = false;
}

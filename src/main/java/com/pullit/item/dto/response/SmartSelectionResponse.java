package com.pullit.item.dto.response;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartSelectionResponse {

    private List<ItemSearchResponse> items;
    private SmartSelectionMetadata metadata;
    private SmartSelectionReport report;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmartSelectionMetadata {
        private int requestedCount;          // 요청된 문항 수
        private int actualItemCount;          // 실제 선택된 문항 수
        private int selectionUnitCount;       // 선택 단위 수 (독립 문항 + 지문 그룹)
        private int passageGroupCount;        // 포함된 지문 그룹 수
        private Map<Long, DifficultyInfo> difficultyDistribution;
        private List<PassageGroupInfo> passageGroups;
        private List<FallbackAction> fallbackActions;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DifficultyInfo {
        private Long difficultyCode;
        private String difficultyName;
        private int targetCount;
        private int actualCount;
        private int independentItems;
        private int passageGroups;
        private double targetPercentage;
        private double actualPercentage;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassageGroupInfo {
        private Long passageId;
        private int itemCount;
        private Long representativeDifficulty;
        private List<Long> itemIds;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FallbackAction {
        private String action;
        private Long fromDifficulty;
        private Long toDifficulty;
        private int count;
        private String reason;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmartSelectionReport {
        private boolean success;
        private String message;
        private List<String> warnings;
        private double distributionAccuracy; // 0-100%
    }
}

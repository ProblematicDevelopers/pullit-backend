package com.pullit.item.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum DifficultyDistribution {
    EASY("easy", Map.of(1L, 0.7, 2L, 0.3, 3L, 0.0)),
    NORMAL("normal", Map.of(1L, 0.2, 2L, 0.6, 3L, 0.2)),
    HARD("hard", Map.of(1L, 0.0, 2L, 0.3, 3L, 0.7)),
    MIXED("mixed", Map.of(1L, 0.33, 2L, 0.34, 3L, 0.33));

    private final String code;
    private final Map<Long, Double> distribution;

    public static DifficultyDistribution fromCode(String code) {
        for (DifficultyDistribution dist : values()) {
            if (dist.code.equalsIgnoreCase(code)) {
                return dist;
            }
        }
        return MIXED;
    }
}

package com.pullit.itemprocess.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DifficultyLevel {
    EASY("easy", "쉬움"),
    MEDIUM("medium", "보통"),
    HARD("hard", "어려움");
    
    private final String code;
    private final String displayName;
    
    public static DifficultyLevel fromCode(String code) {
        for (DifficultyLevel level : values()) {
            if (level.getCode().equals(code)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown DifficultyLevel code: " + code);
    }
}
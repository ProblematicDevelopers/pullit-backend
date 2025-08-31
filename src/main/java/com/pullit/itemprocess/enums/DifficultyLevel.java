package com.pullit.itemprocess.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
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

    // ✅ JSON 역직렬화: 상수명/코드 모두 허용 (e.g. "MEDIUM" 또는 "medium")
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static DifficultyLevel fromJson(String v) {
        if (v == null) return null;
        for (DifficultyLevel d : values()) {
            if (d.name().equalsIgnoreCase(v) || d.code.equalsIgnoreCase(v)) return d;
        }
        throw new IllegalArgumentException("Unknown DifficultyLevel: " + v);
    }

    // ✅ JSON 직렬화: 응답은 상수명으로 통일 (원하면 code로 바꿔도 됨)
    @JsonValue
    public String toJson() {
        return this.name();
    }
}

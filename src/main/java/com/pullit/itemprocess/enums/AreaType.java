package com.pullit.itemprocess.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AreaType {
    PASSAGE("passage", "지문"),
    PROBLEM("problem", "문제"),
    OPTIONS("options", "보기");
    
    private final String code;
    private final String displayName;
    
    public static AreaType fromCode(String code) {
        for (AreaType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown AreaType code: " + code);
    }
    
    // JSON 역직렬화: 상수명/코드 모두 허용 (e.g. "OPTIONS" 또는 "options")
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AreaType fromJson(String v) {
        if (v == null) return null;
        for (AreaType t : values()) {
            if (t.name().equalsIgnoreCase(v) || t.code.equalsIgnoreCase(v)) return t;
        }
        throw new IllegalArgumentException("Unknown AreaType: " + v);
    }

    // JSON 직렬화: 응답은 상수명으로 통일
    @JsonValue
    public String toJson() {
        return this.name();
    }
}
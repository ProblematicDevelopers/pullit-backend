package com.pullit.itemprocess.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ItemType {
    FIVE_CHOICE("fiveChoice", "5지 선택"),
    SHORT_ANSWER_ORDERED("shortAnswerOrdered", "단답 유순형"),
    SHORT_ANSWER_UNORDERED("shortAnswerUnOrdered", "단답 무순형"),
    FREE_CHOICE("freeChoice", "자유 선지형");

    private final String code;
    private final String displayName;

    public static ItemType fromCode(String code) {
        for (ItemType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ItemType code: " + code);
    }

    // ✅ JSON 역직렬화: 상수명/코드 모두 허용 (e.g. "SUBJECTIVE" 또는 "subjective")
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ItemType fromJson(String v) {
        if (v == null) return null;
        for (ItemType t : values()) {
            if (t.name().equalsIgnoreCase(v) || t.code.equalsIgnoreCase(v)) return t;
        }
        throw new IllegalArgumentException("Unknown ItemType: " + v);
    }

    // ✅ JSON 직렬화: 응답은 상수명으로 통일 (원하면 code로 바꿔도 됨)
    @JsonValue
    public String toJson() {
        return this.name();
    }
}

package com.pullit.itemprocess.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ItemType {
    MULTIPLE("multiple", "객관식"),
    SUBJECTIVE("subjective", "주관식"),
    SHORT_ANSWER("shortAnswer", "단답형"),
    ESSAY("essay", "서술형");
    
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
}
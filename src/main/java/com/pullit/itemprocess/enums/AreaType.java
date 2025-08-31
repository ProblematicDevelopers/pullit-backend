package com.pullit.itemprocess.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AreaType {
    QUESTION("question", "지문"),
    PROBLEM("problem", "문제"),
    IMAGE("image", "이미지"),
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
}
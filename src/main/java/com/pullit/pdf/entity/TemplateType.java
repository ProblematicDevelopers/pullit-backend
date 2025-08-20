package com.pullit.pdf.entity;

import lombok.Getter;

@Getter
public enum TemplateType {
    EXAM("시험지"),
    ANSWER_SHEET("답안지"),
    COMBINED("통합"),
    CUSTOM("사용자정의");

    private final String description;

    TemplateType(String description) {
        this.description = description;
    }

}

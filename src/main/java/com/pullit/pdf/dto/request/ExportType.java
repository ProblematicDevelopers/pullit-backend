package com.pullit.pdf.dto.request;


import lombok.Getter;

@Getter
public enum ExportType {
    QUESTION_ONLY("문제지만"),
    ANSWER_ONLY("답안지만"),
    COMBINED("통합본"),
    SEPARATE("분리");

    private final String description;

    ExportType(String description) {
        this.description = description;
    }

}

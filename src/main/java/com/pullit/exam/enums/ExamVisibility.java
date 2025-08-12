package com.pullit.exam.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExamVisibility {
    PRIVATE("비공개", "시험 생성자만 접근 가능"),
    SCHOOL("학교 공개", "같은 학교 구성원만 접근 가능"),
    PUBLIC("전체 공개", "모든 사용자 접근 가능");

    private final String title;
    private final String description;

    public boolean isPublic() {
        return this == PUBLIC;
    }

    public boolean isSchoolOnly() {
        return this == SCHOOL;
    }

    public boolean isPrivate() {
        return this == PRIVATE;
    }

}

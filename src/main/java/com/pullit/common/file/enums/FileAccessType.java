package com.pullit.common.file.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FileAccessType {
    
    VIEW("조회", "파일 정보 조회"),
    DOWNLOAD("다운로드", "파일 다운로드"),
    DELETE("삭제", "파일 삭제"),
    UPDATE("수정", "파일 정보 수정"),
    SHARE("공유", "파일 공유");
    
    private final String korean;
    private final String description;
}
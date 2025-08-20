package com.pullit.common.file.enums;

import com.pullit.common.s3.enums.S3Directory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FileCategory {
    
    DOCUMENT("문서", "일반 문서 파일"),
    IMAGE("이미지", "이미지 파일"),
    VIDEO("동영상", "동영상 파일"),
    AUDIO("오디오", "오디오 파일"),
    
    EXAM("시험지", "시험 문제 파일"),
    QUESTION_IMAGE("문제 이미지", "문제에 포함된 이미지"),
    ANSWER_IMAGE("답안 이미지", "답안에 포함된 이미지"),
    EXPLANATION_IMAGE("해설 이미지", "해설에 포함된 이미지"),
    
    USER_PROFILE("프로필 이미지", "사용자 프로필 이미지"),
    USER_DOCUMENT("사용자 문서", "사용자가 업로드한 문서"),
    
    TEMP("임시", "임시 파일"),
    OTHER("기타", "기타 파일");
    
    private final String korean;
    private final String description;
    
    /**
     * 파일 확장자로 카테고리 자동 판별
     */
    public static FileCategory fromContentType(String contentType) {
        if (contentType == null) return OTHER;
        
        if (contentType.startsWith("image/")) return IMAGE;
        if (contentType.startsWith("video/")) return VIDEO;
        if (contentType.startsWith("audio/")) return AUDIO;
        if (contentType.contains("pdf") || 
            contentType.contains("document") || 
            contentType.contains("text")) return DOCUMENT;
            
        return OTHER;
    }
    
    /**
     * S3 디렉토리 매핑
     */
    public S3Directory toS3Directory() {
        return switch (this) {
            case EXAM -> S3Directory.PDF_EXAM;
            case IMAGE, QUESTION_IMAGE, ANSWER_IMAGE, EXPLANATION_IMAGE -> S3Directory.IMAGE_QUESTION;
            case USER_PROFILE -> S3Directory.IMAGE_PROFILE;
            case TEMP -> S3Directory.TEMP;
            default -> S3Directory.DOCUMENT;
        };
    }
}
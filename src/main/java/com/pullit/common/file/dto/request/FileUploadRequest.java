package com.pullit.common.file.dto.request;

import com.pullit.common.file.enums.FileCategory;
import com.pullit.common.s3.enums.S3Directory;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadRequest {
    
    // 파일 분류
    private FileCategory category;
    
    private S3Directory directory;
    
    // 연관 엔티티
    private String entityType;
    
    private Long entityId;
    
    // 파일 설명
    @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
    private String description;
    
    // 태그
    private List<String> tags;
    
    // 추가 메타데이터
    private Map<String, Object> metadata;
    
    // 공개 여부
    @Builder.Default
    private boolean isPublic = false;
    
    // 만료 시간 (임시 파일용, 시간 단위)
    private Integer expirationHours;
    
    // 클라이언트 정보
    private String clientIp;
    
    /**
     * 디렉토리 자동 설정
     */
    public S3Directory getDirectory() {
        if (directory != null) {
            return directory;
        }
        if (category != null) {
            return category.toS3Directory();
        }
        return S3Directory.TEMP;
    }
}
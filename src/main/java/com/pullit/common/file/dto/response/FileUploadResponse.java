package com.pullit.common.file.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pullit.common.file.entity.FileMetadata;
import com.pullit.common.file.enums.FileCategory;
import com.pullit.common.s3.dto.S3UploadResponse;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadResponse {
    
    // 파일 ID
    private Long fileId;
    
    // S3 정보
    private String s3Key;
    private String presignedUrl;
    private String publicUrl;
    
    // 파일 정보
    private String originalFilename;
    private Long fileSize;
    private String contentType;
    private FileCategory category;
    
    // 메타데이터
    private String description;
    private List<String> tags;
    
    // 업로드 정보
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadedAt;
    
    // 만료 정보
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;
    
    /**
     * Entity와 S3Response로부터 생성
     */
    public static FileUploadResponse from(FileMetadata metadata, S3UploadResponse s3Response) {
        return FileUploadResponse.builder()
            .fileId(metadata.getId())
            .s3Key(s3Response.getS3Key())
            .presignedUrl(s3Response.getPresignedUrl())
            .publicUrl(s3Response.getPublicUrl())
            .originalFilename(metadata.getOriginalFilename())
            .fileSize(metadata.getFileSize())
            .contentType(metadata.getContentType())
            .category(metadata.getFileCategory())
            .description(metadata.getDescription())
            .tags(metadata.getTags())
            .uploadedAt(metadata.getCreatedDate())
            .expiresAt(metadata.getExpiresAt())
            .build();
    }
}
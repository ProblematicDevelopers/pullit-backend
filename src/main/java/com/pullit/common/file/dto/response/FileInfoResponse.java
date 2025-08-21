package com.pullit.common.file.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pullit.common.file.entity.FileMetadata;
import com.pullit.common.file.enums.FileCategory;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileInfoResponse {
    
    private Long id;
    private String originalFilename;
    private Long fileSize;
    private String contentType;
    private FileCategory category;
    
    private String description;
    private List<String> tags;
    
    private Boolean isPublic;
    private Boolean isDeleted;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;
    
    private Long uploadedBy;
    private String entityType;
    private Long entityId;
    
    // Pre-signed URL (요청 시 생성)
    private String downloadUrl;
    
    /**
     * Entity로부터 Response 생성
     */
    public static FileInfoResponse from(FileMetadata metadata) {
        return FileInfoResponse.builder()
            .id(metadata.getId())
            .originalFilename(metadata.getOriginalFilename())
            .fileSize(metadata.getFileSize())
            .contentType(metadata.getContentType())
            .category(metadata.getFileCategory())
            .description(metadata.getDescription())
            .tags(metadata.getTags())
            .isPublic(metadata.getIsPublic())
            .isDeleted(metadata.getIsDeleted())
            .createdAt(metadata.getCreatedDate())
            .updatedAt(metadata.getUpdatedDate())
            .expiresAt(metadata.getExpiresAt())
            .uploadedBy(metadata.getUploadedBy())
            .entityType(metadata.getEntityType())
            .entityId(metadata.getEntityId())
            .build();
    }
    
    /**
     * 다운로드 URL 설정
     */
    public FileInfoResponse withDownloadUrl(String url) {
        this.downloadUrl = url;
        return this;
    }
}
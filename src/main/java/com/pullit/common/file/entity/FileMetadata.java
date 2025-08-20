package com.pullit.common.file.entity;

import com.pullit.common.entity.BaseTimeEntity;
import com.pullit.common.file.enums.FileCategory;
import com.pullit.common.s3.enums.S3Directory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "file_metadata", indexes = {
    @Index(name = "idx_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_uploaded_by", columnList = "uploaded_by"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_file_category", columnList = "file_category"),
    @Index(name = "idx_is_deleted", columnList = "is_deleted")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"metadata", "tags"})
public class FileMetadata extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // S3 정보
    @Column(name = "s3_key", nullable = false, unique = true, length = 500)
    private String s3Key;
    
    @Column(name = "s3_url", length = 1000)
    private String s3Url;
    
    @Column(name = "bucket_name", length = 100)
    private String bucketName;
    
    // 파일 정보
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "content_type", length = 100)
    private String contentType;
    
    @Column(name = "file_extension", length = 20)
    private String fileExtension;
    
    // 분류
    @Enumerated(EnumType.STRING)
    @Column(name = "file_category", length = 50)
    private FileCategory fileCategory;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "directory_type", length = 50)
    private S3Directory directoryType;
    
    // 업로드 정보
    @Column(name = "uploaded_by")
    private Long uploadedBy;
    
    @Column(name = "upload_ip", length = 45)
    private String uploadIp;
    
    // 다형성 관계 (어떤 엔티티와 연결되는지)
    @Column(name = "entity_type", length = 50)
    private String entityType;
    
    @Column(name = "entity_id")
    private Long entityId;
    
    // 메타데이터
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private List<String> tags;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private Map<String, Object> metadata;
    
    // 상태
    @Builder.Default
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;
    
    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    // 비즈니스 로직
    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        // 30일 후 실제 삭제 예약
        this.expiresAt = LocalDateTime.now().plusDays(30);
    }
    
    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
        this.expiresAt = null;
    }
    
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean canAccess(Long userId) {
        // 공개 파일이거나 업로더 본인인 경우 접근 가능
        return isPublic || (uploadedBy != null && uploadedBy.equals(userId));
    }
}
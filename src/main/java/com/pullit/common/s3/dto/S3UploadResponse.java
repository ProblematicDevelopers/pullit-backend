package com.pullit.common.s3.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class S3UploadResponse {
    private String s3Key;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private Instant uploadedAt;
    private String etag;
    private String presignedUrl;  // 임시 다운로드 URL
    private String publicUrl;     // public 파일인 경우 영구 URL
}

package com.pullit.common.s3.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class S3FileInfo {
    private String s3Key;
    private Long fileSize;
    private String contentType;
    private Instant lastModified;
    private String etag;
    private Map<String, String> metadata;
}

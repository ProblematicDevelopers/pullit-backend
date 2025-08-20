package com.pullit.common.s3.dto;

import com.pullit.common.s3.enums.S3Directory;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * S3 업로드 요청 DTO
 * 커스텀 업로드 설정이 필요한 경우 사용
 */
@Data
@Builder
public class S3UploadRequest {
    private byte[] fileData;
    private String fileName;
    private S3Directory directory;
    private String contentType;
    private Map<String, String> metadata;
    private Map<String, String> tags;
    private boolean publicRead;  // public 접근 허용 여부
}
package com.pullit.common.s3.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aws.s3")
public class S3Properties {
    private String accessKey;
    private String secretKey;
    private String region = "ap-northeast-2";
    private String bucketName;

    // 각 도메인별 기본 경로 설정
    private Directories directories = new Directories();

    // Pre-signed URL 기본 유효시간 (분 단위)
    private Integer defaultPresignedUrlExpiry = 60;

    // 최대 파일 크기 (MB)
    private Integer maxFileSize = 100;

    @Data
    public static class Directories {
        private String pdf = "pdfs/";
        private String image = "images/";
        private String excel = "excels/";
        private String temp = "temp/";
        private String profile = "profiles/";
    }
}

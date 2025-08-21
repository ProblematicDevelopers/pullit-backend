package com.pullit.common.s3.service;

import com.pullit.common.s3.dto.S3FileInfo;
import com.pullit.common.s3.dto.S3UploadRequest;
import com.pullit.common.s3.dto.S3UploadResponse;
import com.pullit.common.s3.enums.S3Directory;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

public interface S3Service {

    /**
     * 파일 업로드 (바이트 배열)
     */
    S3UploadResponse upload(byte[] fileData, String fileName, S3Directory directory);

    /**
     * 파일 업로드 (MultipartFile)
     */
    S3UploadResponse upload(MultipartFile file, S3Directory directory);

    /**
     * 파일 업로드 (커스텀 설정)
     */
    S3UploadResponse upload(S3UploadRequest request);

    /**
     * 파일 다운로드
     */
    byte[] download(String s3Key);

    /**
     * Pre-signed URL 생성 (기본 1시간)
     */
    String generatePresignedUrl(String s3Key);

    /**
     * Pre-signed URL 생성 (커스텀 유효시간)
     */
    String generatePresignedUrl(String s3Key, Duration duration);

    /**
     * 파일 삭제
     */
    void delete(String s3Key);

    /**
     * 파일 일괄 삭제
     */
    void deleteMultiple(List<String> s3Keys);

    /**
     * 파일 존재 여부 확인
     */
    boolean exists(String s3Key);

    /**
     * 파일 정보 조회
     */
    S3FileInfo getFileInfo(String s3Key);

    /**
     * 파일 복사
     */
    String copy(String sourceKey, String targetKey);

    /**
     * 파일 이동
     */
    String move(String sourceKey, String targetKey);

    /**
     * 임시 파일 정리 (오래된 temp 파일 삭제)
     */
    void cleanupTempFiles(int daysOld);

}

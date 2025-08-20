package com.pullit.common.s3.service;

import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;
import com.pullit.common.s3.config.S3Properties;
import com.pullit.common.s3.dto.S3FileInfo;
import com.pullit.common.s3.dto.S3UploadRequest;
import com.pullit.common.s3.dto.S3UploadResponse;
import com.pullit.common.s3.enums.S3Directory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class S3ServiceImpl implements S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    @Override
    public S3UploadResponse upload(byte[] fileData, String fileName, S3Directory directory) {
        // S3UploadRequest 빌더로 생성
        S3UploadRequest request = S3UploadRequest.builder()
                .fileData(fileData)
                .fileName(fileName)
                .directory(directory)
                .build();

        return upload(request);
    }

    @Override
    public S3UploadResponse upload(MultipartFile file, S3Directory directory) {
        try {
            return upload(file.getBytes(), file.getOriginalFilename(), directory);
        } catch (IOException e) {
            log.error("MultipartFile 읽기 실패: {}", file.getOriginalFilename(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public S3UploadResponse upload(S3UploadRequest request) {
        validateUploadRequest(request);
        
        log.info("S3 업로드 시작: 파일명={}, 크기={} bytes, 디렉토리={}", 
            request.getFileName(), request.getFileData().length, request.getDirectory());

        // S3 키 생성
        String s3Key = generateS3Key(request.getFileName(), request.getDirectory());

        try {
            // 메타데이터 설정
            Map<String, String> metadata = request.getMetadata();
            if (metadata == null) {
                metadata = new HashMap<>();
            }
            // 한글 파일명은 URL 인코딩하여 메타데이터에 저장
            String encodedFileName = URLEncoder.encode(request.getFileName(), StandardCharsets.UTF_8);
            metadata.put("original-filename", encodedFileName);
            metadata.put("upload-time", Instant.now().toString());

            // S3 업로드 요청 빌드
            PutObjectRequest.Builder putRequestBuilder = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(s3Key)
                    .contentType(detectContentType(request))
                    .metadata(metadata);
            
            // Content-Disposition 헤더 - 서명 오류 방지를 위해 제거
            // 원본 파일명은 메타데이터에서 관리
            // putRequestBuilder.contentDisposition("attachment");

            // 태그 설정
            if (request.getTags() != null && !request.getTags().isEmpty()) {
                putRequestBuilder.tagging(buildTagging(request.getTags()));
            }

            // ACL 설정 (public-read 여부)
            // 주석 처리 - ACL 설정 권한 문제로 임시 비활성화
            // if (request.isPublicRead()) {
            //     putRequestBuilder.acl(ObjectCannedACL.PUBLIC_READ);
            // }

            PutObjectRequest putRequest = putRequestBuilder.build();

            // S3 업로드 실행
            PutObjectResponse response = s3Client.putObject(
                    putRequest,
                    RequestBody.fromBytes(request.getFileData())
            );

            log.info("S3 업로드 성공: key={}, size={} bytes, etag={}",
                    s3Key, request.getFileData().length, response.eTag());

            // 응답 생성
            return S3UploadResponse.builder()
                    .s3Key(s3Key)
                    .fileName(request.getFileName())
                    .fileSize((long) request.getFileData().length)
                    .contentType(detectContentType(request))
                    .uploadedAt(Instant.now())
                    .etag(response.eTag())
                    .presignedUrl(generatePresignedUrl(s3Key))
                    .publicUrl(request.isPublicRead() ? getPublicUrl(s3Key) : null)
                    .build();

        } catch (Exception e) {
            log.error("S3 업로드 실패: fileName={}, key={}", request.getFileName(), s3Key, e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public byte[] download(String s3Key) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(s3Key)
                    .build();

            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(getRequest);

            log.info("S3 다운로드 성공: key={}, size={} bytes",
                    s3Key, response.asByteArray().length);

            return response.asByteArray();

        } catch (NoSuchKeyException e) {
            log.error("S3 파일 없음: key={}", s3Key);
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        } catch (Exception e) {
            log.error("S3 다운로드 실패: key={}", s3Key, e);
            throw new BusinessException(ErrorCode.FILE_DOWNLOAD_FAILED);
        }
    }

    @Override
    public String generatePresignedUrl(String s3Key) {
        return generatePresignedUrl(s3Key,
                Duration.ofMinutes(s3Properties.getDefaultPresignedUrlExpiry()));
    }

    @Override
    public String generatePresignedUrl(String s3Key, Duration duration) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(s3Key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(duration)
                    .getObjectRequest(getRequest)
                    .build();

            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);

            return presigned.url().toString();

        } catch (Exception e) {
            log.error("Pre-signed URL 생성 실패: key={}", s3Key, e);
            throw new BusinessException(ErrorCode.S3_OPERATION_FAILED);
        }
    }

    @Override
    public void delete(String s3Key) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("S3 파일 삭제 성공: key={}", s3Key);

        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: key={}", s3Key, e);
            throw new BusinessException(ErrorCode.S3_OPERATION_FAILED);
        }
    }

    @Override
    public void deleteMultiple(List<String> s3Keys) {
        if (s3Keys == null || s3Keys.isEmpty()) return;

        try {
            // 삭제할 객체 목록 생성
            List<ObjectIdentifier> objects = s3Keys.stream()
                    .map(key -> ObjectIdentifier.builder().key(key).build())
                    .toList();

            Delete delete = Delete.builder()
                    .objects(objects)
                    .build();

            DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .delete(delete)
                    .build();

            DeleteObjectsResponse response = s3Client.deleteObjects(deleteRequest);

            log.info("S3 파일 일괄 삭제 성공: count={}, deleted={}",
                    s3Keys.size(), response.deleted().size());

        } catch (Exception e) {
            log.error("S3 파일 일괄 삭제 실패", e);
            throw new BusinessException(ErrorCode.S3_OPERATION_FAILED);
        }
    }

    @Override
    public boolean exists(String s3Key) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(s3Key)
                    .build();

            s3Client.headObject(headRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("S3 파일 존재 확인 실패: key={}", s3Key, e);
            throw new BusinessException(ErrorCode.S3_OPERATION_FAILED);
        }
    }

    @Override
    public S3FileInfo getFileInfo(String s3Key) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(s3Key)
                    .build();

            HeadObjectResponse response = s3Client.headObject(headRequest);

            return S3FileInfo.builder()
                    .s3Key(s3Key)
                    .fileSize(response.contentLength())
                    .contentType(response.contentType())
                    .lastModified(response.lastModified())
                    .etag(response.eTag())
                    .metadata(response.metadata())
                    .build();

        } catch (NoSuchKeyException e) {
            log.error("S3 파일 없음: key={}", s3Key);
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        } catch (Exception e) {
            log.error("S3 파일 정보 조회 실패: key={}", s3Key, e);
            throw new BusinessException(ErrorCode.S3_OPERATION_FAILED);
        }
    }

    @Override
    public String copy(String sourceKey, String targetKey) {
        try {
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(s3Properties.getBucketName())
                    .sourceKey(sourceKey)
                    .destinationBucket(s3Properties.getBucketName())
                    .destinationKey(targetKey)
                    .build();

            CopyObjectResponse response = s3Client.copyObject(copyRequest);

            log.info("S3 파일 복사 성공: {} -> {}", sourceKey, targetKey);

            return targetKey;

        } catch (Exception e) {
            log.error("S3 파일 복사 실패: {} -> {}", sourceKey, targetKey, e);
            throw new BusinessException(ErrorCode.S3_OPERATION_FAILED);
        }
    }

    @Override
    public String move(String sourceKey, String targetKey) {
        // 복사 후 원본 삭제
        copy(sourceKey, targetKey);
        delete(sourceKey);

        log.info("S3 파일 이동 성공: {} -> {}", sourceKey, targetKey);

        return targetKey;
    }

    @Override
    public void cleanupTempFiles(int daysOld) {
        // temp 폴더의 오래된 파일 삭제
        String prefix = S3Directory.TEMP.getPath();

        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(s3Properties.getBucketName())
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);

            Instant cutoffTime = Instant.now().minus(Duration.ofDays(daysOld));

            List<String> keysToDelete = response.contents().stream()
                    .filter(obj -> obj.lastModified().isBefore(cutoffTime))
                    .map(S3Object::key)
                    .toList();

            if (!keysToDelete.isEmpty()) {
                deleteMultiple(keysToDelete);
                log.info("임시 파일 정리 완료: {}개 파일 삭제", keysToDelete.size());
            }

        } catch (Exception e) {
            log.error("임시 파일 정리 실패", e);
            // 정리 실패는 치명적이지 않으므로 예외를 던지지 않음
        }
    }

    /**
     * S3 키 생성
     */
    private String generateS3Key(String fileName, S3Directory directory) {
        String dateBasedPath = directory.getDateBasedPath();
        String uniqueId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String cleanFileName = sanitizeFileName(fileName);
        
        // 언더스코어 대신 하이픈 사용 (S3 서명 안정성)
        String s3Key = dateBasedPath + uniqueId + "-" + cleanFileName;
        
        log.debug("S3 키 생성: 원본파일명={}, 정제된파일명={}, 최종키={}", 
            fileName, cleanFileName, s3Key);

        return s3Key;
    }

    /**
     * 파일명 정제 - S3 키용
     * 한글 파일명은 해시 기반으로 처리하여 안전한 S3 키 생성
     * 원본 파일명은 메타데이터에 저장됨
     */
    private String sanitizeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "unnamed-file";
        }
        
        // 확장자 추출
        String extension = "";
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            extension = fileName.substring(lastDotIndex).toLowerCase();
        }
        
        String nameWithoutExt = fileName;
        if (!extension.isEmpty()) {
            nameWithoutExt = fileName.substring(0, lastDotIndex);
        }
        
        // 한글이나 특수문자가 포함된 경우 해시 기반 파일명 생성
        if (containsNonAlphanumeric(nameWithoutExt)) {
            // 원본 파일명의 해시값 생성 (충돌 방지)
            String hash = generateHash(fileName); // 전체 파일명으로 해시 생성
            
            // 영문/숫자만 추출
            String alphanumeric = nameWithoutExt.replaceAll("[^a-zA-Z0-9]", "");
            
            if (alphanumeric.isEmpty()) {
                // 한글/특수문자만 있는 경우
                return "file-" + hash.substring(0, 8) + extension;
            } else {
                // 혼합된 경우: 영문-해시
                if (alphanumeric.length() > 10) {
                    alphanumeric = alphanumeric.substring(0, 10);
                }
                return alphanumeric + "-" + hash.substring(0, 6) + extension;
            }
        }
        
        // 영문/숫자만 있는 경우
        String sanitized = nameWithoutExt.replaceAll("[^a-zA-Z0-9]", "-");
        
        // 연속된 하이픈을 하나로
        sanitized = sanitized.replaceAll("-+", "-");
        
        // 앞뒤 하이픈 제거
        sanitized = sanitized.replaceAll("^-|-$", "");
        
        // 빈 문자열이면 기본값
        if (sanitized.isEmpty()) {
            sanitized = "file";
        }
        
        // 길이 제한 (30자로 줄임)
        if (sanitized.length() > 30) {
            sanitized = sanitized.substring(0, 30);
        }
        
        return sanitized + extension;
    }
    
    /**
     * 영문/숫자 이외의 문자 포함 여부 확인
     */
    private boolean containsNonAlphanumeric(String text) {
        return !text.matches("^[a-zA-Z0-9]+$");
    }
    
    /**
     * 한글 포함 여부 확인
     */
    private boolean containsKorean(String text) {
        return text.matches(".*[가-힣]+.*");
    }
    
    /**
     * SHA-256 해시 생성 (처음 16자만 사용)
     */
    private String generateHash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            // fallback to UUID
            return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
    }
    
    /**
     * 바이트 배열을 헥스 문자열로 변환
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Content-Type 감지
     */
    private String detectContentType(S3UploadRequest request) {
        if (StringUtils.hasText(request.getContentType())) {
            return request.getContentType();
        }

        String fileName = request.getFileName().toLowerCase();

        if (fileName.endsWith(".pdf")) return "application/pdf";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".gif")) return "image/gif";
        if (fileName.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (fileName.endsWith(".xls")) return "application/vnd.ms-excel";
        if (fileName.endsWith(".doc")) return "application/msword";
        if (fileName.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

        return "application/octet-stream";
    }

    /**
     * 태깅 빌드
     */
    private Tagging buildTagging(Map<String, String> tags) {
        List<Tag> tagList = tags.entrySet().stream()
                .map(entry -> Tag.builder()
                        .key(entry.getKey())
                        .value(entry.getValue())
                        .build())
                .toList();

        return Tagging.builder()
                .tagSet(tagList)
                .build();
    }

    /**
     * Public URL 생성
     */
    private String getPublicUrl(String s3Key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                s3Properties.getBucketName(),
                s3Properties.getRegion(),
                s3Key);
    }

    /**
     * 업로드 요청 검증
     */
    private void validateUploadRequest(S3UploadRequest request) {
        if (request.getFileData() == null || request.getFileData().length == 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // 파일 크기 검증 (MB 단위)
        long fileSizeMB = request.getFileData().length / (1024 * 1024);
        if (fileSizeMB > s3Properties.getMaxFileSize()) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
    }
}

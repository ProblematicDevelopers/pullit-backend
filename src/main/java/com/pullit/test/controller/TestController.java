package com.pullit.test.controller;

import com.pullit.common.dto.response.ApiResponse;
import com.pullit.common.exception.ErrorCode;
import com.pullit.common.s3.dto.S3FileInfo;
import com.pullit.common.s3.dto.S3UploadResponse;
import com.pullit.common.s3.enums.S3Directory;
import com.pullit.common.s3.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Tag(name = "Test API", description = "API 연동 테스트용 엔드포인트")
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {
    
    private final S3Service s3Service;
    
    @Operation(summary = "헬스 체크", description = "서버 상태를 확인합니다")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.info("Health check called");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Backend server is runningaaaaaabbbbb");
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "에코 테스트", description = "요청한 데이터를 그대로 반환합니다")
    @PostMapping("/echo")
    public ResponseEntity<Map<String, Object>> echo(@RequestBody Map<String, Object> request) {
        log.info("Echo endpoint called with data: {}", request);

        
        Map<String, Object> response = new HashMap<>();
        response.put("received", request);
        response.put("message", "Echo successful");
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "CORS 테스트", description = "CORS 설정이 올바른지 확인합니다")
    @GetMapping("/cors-test")
    public ResponseEntity<Map<String, Object>> corsTest() {
        log.info("CORS test endpoint called");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "안녕~~");
        response.put("backend", "Spring Boot");
        response.put("frontend", "Vue.js");
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    // ========== S3 테스트 엔드포인트 ==========
    
    @Operation(summary = "S3 텍스트 업로드 테스트", description = "간단한 텍스트를 S3에 업로드합니다")
    @PostMapping("/s3/upload-text")
    public ResponseEntity<ApiResponse<S3UploadResponse>> uploadTextToS3(
            @RequestParam(defaultValue = "Hello S3! 안녕하세요!") String content
    ) {
        try {
            String fileName = "test_" + System.currentTimeMillis() + ".txt";
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            
            log.info("S3 텍스트 업로드 시작: fileName={}, size={} bytes", fileName, bytes.length);
            
            S3UploadResponse response = s3Service.upload(
                bytes,
                fileName,
                S3Directory.TEMP
            );
            
            log.info("S3 업로드 성공: s3Key={}, publicUrl={}", 
                response.getS3Key(), response.getPublicUrl());
            
            return ResponseEntity.ok(
                ApiResponse.success(response, "S3 업로드 성공")
            );
        } catch (Exception e) {
            log.error("S3 업로드 실패", e);
            return ResponseEntity.ok(
                ApiResponse.error(ErrorCode.FILE_UPLOAD_FAILED)
            );
        }
    }
    
    @Operation(
        summary = "S3 파일 업로드 테스트", 
        description = "파일을 S3에 업로드합니다"
    )
    @PostMapping(value = "/s3/upload-file", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<S3UploadResponse>> uploadFileToS3(
            @RequestParam("file") MultipartFile file
    ) {
        try {
            log.info("S3 파일 업로드 시작: fileName={}, size={} bytes, contentType={}", 
                file.getOriginalFilename(), file.getSize(), file.getContentType());
            
            // 파일 타입에 따라 디렉토리 결정
            S3Directory directory = S3Directory.TEMP;
            String contentType = file.getContentType();
            
            if (contentType != null) {
                if (contentType.startsWith("image/")) {
                    directory = S3Directory.IMAGE_QUESTION;
                    log.info("이미지 파일로 감지, IMAGE_QUESTION 디렉토리 사용");
                } else if (contentType.equals("application/pdf")) {
                    directory = S3Directory.PDF_EXAM;
                    log.info("PDF 파일로 감지, PDF_EXAM 디렉토리 사용");
                }
            }
            
            S3UploadResponse response = s3Service.upload(file, directory);
            
            log.info("S3 파일 업로드 성공: s3Key={}, presignedUrl={}", 
                response.getS3Key(), response.getPresignedUrl());
            
            return ResponseEntity.ok(
                ApiResponse.success(response, "파일 업로드 성공")
            );
        } catch (Exception e) {
            log.error("S3 파일 업로드 실패: {}", file.getOriginalFilename(), e);
            return ResponseEntity.ok(
                ApiResponse.error(ErrorCode.FILE_UPLOAD_FAILED)
            );
        }
    }
    
    @Operation(summary = "S3 파일 다운로드", description = "S3에서 파일을 다운로드합니다")
    @GetMapping("/s3/download")
    public ResponseEntity<byte[]> downloadFromS3(
            @RequestParam String s3Key
    ) {
        try {
            log.info("S3 다운로드 시작: s3Key={}", s3Key);
            
            // 파일 데이터 다운로드
            byte[] data = s3Service.download(s3Key);
            
            // 파일 정보 가져오기 (원본 파일명 포함)
            S3FileInfo fileInfo = s3Service.getFileInfo(s3Key);
            
            // 메타데이터에서 원본 파일명 추출
            String originalFileName = "download.bin"; // 기본값
            if (fileInfo.getMetadata() != null && fileInfo.getMetadata().containsKey("original-filename")) {
                // URL 디코딩하여 원본 파일명 복원
                String encodedName = fileInfo.getMetadata().get("original-filename");
                try {
                    originalFileName = java.net.URLDecoder.decode(encodedName, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    log.warn("파일명 디코딩 실패, 기본값 사용: {}", encodedName);
                    // S3 키에서 파일명 추출
                    int lastSlash = s3Key.lastIndexOf('/');
                    if (lastSlash >= 0 && lastSlash < s3Key.length() - 1) {
                        originalFileName = s3Key.substring(lastSlash + 1);
                    }
                }
            } else {
                // 메타데이터가 없으면 S3 키에서 파일명 추출
                int lastSlash = s3Key.lastIndexOf('/');
                if (lastSlash >= 0 && lastSlash < s3Key.length() - 1) {
                    originalFileName = s3Key.substring(lastSlash + 1);
                }
            }
            
            log.info("S3 다운로드 성공: s3Key={}, size={} bytes, fileName={}", 
                s3Key, data.length, originalFileName);
            
            // Content-Type 설정
            String contentType = fileInfo.getContentType() != null ? 
                fileInfo.getContentType() : "application/octet-stream";
            
            // 파일명 처리 - ASCII만 사용
            String safeFileName;
            if (isAsciiPrintable(originalFileName)) {
                // ASCII 문자만 있는 경우 그대로 사용
                safeFileName = originalFileName;
            } else {
                // 한글 등 non-ASCII 문자가 있는 경우
                // 확장자 추출
                String extension = "";
                int lastDot = originalFileName.lastIndexOf('.');
                if (lastDot > 0) {
                    extension = originalFileName.substring(lastDot);
                }
                // 안전한 파일명 생성
                safeFileName = "download" + extension;
            }
            
            // RFC 5987 형식으로 파일명 인코딩 (한글 파일명 지원)
            String encodedFileName = URLEncoder.encode(originalFileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
            
            return ResponseEntity.ok()
                .header("Content-Disposition", 
                    "attachment; filename=\"" + safeFileName + "\"; " +
                    "filename*=UTF-8''" + encodedFileName)
                .header("Content-Type", contentType)
                .header("Content-Length", String.valueOf(data.length))
                .body(data);
                
        } catch (Exception e) {
            log.error("S3 다운로드 실패: s3Key={}", s3Key, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @Operation(summary = "S3 Pre-signed URL 생성", description = "임시 다운로드 URL을 생성합니다")
    @GetMapping("/s3/presigned-url")
    public ResponseEntity<ApiResponse<Map<String, String>>> getPresignedUrl(
            @RequestParam String s3Key
    ) {
        try {
            log.info("Pre-signed URL 생성 요청: s3Key={}", s3Key);
            
            String url = s3Service.generatePresignedUrl(s3Key);
            
            Map<String, String> result = new HashMap<>();
            result.put("s3Key", s3Key);
            result.put("presignedUrl", url);
            result.put("expiresIn", "60 minutes");
            
            log.info("Pre-signed URL 생성 성공");
            
            return ResponseEntity.ok(
                ApiResponse.success(result, "Pre-signed URL 생성 성공")
            );
        } catch (Exception e) {
            log.error("Pre-signed URL 생성 실패: s3Key={}", s3Key, e);
            return ResponseEntity.ok(
                ApiResponse.error(ErrorCode.S3_OPERATION_FAILED)
            );
        }
    }
    
    @Operation(summary = "S3 파일 존재 확인", description = "파일이 S3에 존재하는지 확인합니다")
    @GetMapping("/s3/exists")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkS3FileExists(
            @RequestParam String s3Key
    ) {
        try {
            log.info("S3 파일 존재 확인: s3Key={}", s3Key);
            
            boolean exists = s3Service.exists(s3Key);
            
            Map<String, Object> result = new HashMap<>();
            result.put("s3Key", s3Key);
            result.put("exists", exists);
            
            if (exists) {
                // 파일이 있으면 정보도 가져오기
                S3FileInfo fileInfo = s3Service.getFileInfo(s3Key);
                result.put("fileSize", fileInfo.getFileSize());
                result.put("lastModified", fileInfo.getLastModified());
                result.put("contentType", fileInfo.getContentType());
            }
            
            return ResponseEntity.ok(
                ApiResponse.success(result, 
                    exists ? "파일이 존재합니다" : "파일이 없습니다")
            );
        } catch (Exception e) {
            log.error("S3 파일 확인 실패: s3Key={}", s3Key, e);
            return ResponseEntity.ok(
                ApiResponse.error(ErrorCode.S3_OPERATION_FAILED)
            );
        }
    }
    
    @Operation(summary = "S3 파일 삭제", description = "S3에서 파일을 삭제합니다")
    @DeleteMapping("/s3/delete")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteFromS3(
            @RequestParam String s3Key
    ) {
        try {
            log.info("S3 파일 삭제 요청: s3Key={}", s3Key);
            
            // 삭제 전 확인
            boolean existsBefore = s3Service.exists(s3Key);
            
            if (!existsBefore) {
                return ResponseEntity.ok(
                    ApiResponse.error(ErrorCode.FILE_NOT_FOUND)
                );
            }
            
            s3Service.delete(s3Key);
            
            // 삭제 후 확인
            boolean existsAfter = s3Service.exists(s3Key);
            
            Map<String, String> result = new HashMap<>();
            result.put("s3Key", s3Key);
            result.put("deleted", String.valueOf(!existsAfter));
            
            log.info("S3 파일 삭제 완료: s3Key={}", s3Key);
            
            return ResponseEntity.ok(
                ApiResponse.success(result, "파일 삭제 성공")
            );
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: s3Key={}", s3Key, e);
            return ResponseEntity.ok(
                ApiResponse.error(ErrorCode.FILE_DELETE_FAILED)
            );
        }
    }
    
    /**
     * ASCII 출력 가능 문자인지 확인
     */
    private boolean isAsciiPrintable(String str) {
        if (str == null) return false;
        for (char c : str.toCharArray()) {
            if (c < 32 || c > 126) {
                return false;
            }
        }
        return true;
    }
    
    @Operation(summary = "S3 퍼블릭 URL 테스트", description = "퍼블릭 파일의 영구 URL을 테스트합니다")
    @PostMapping("/s3/test-public")
    public ResponseEntity<ApiResponse<Map<String, String>>> testPublicUpload(
            @RequestParam(defaultValue = "This is a public test file!") String content
    ) {
        try {
            String fileName = "public_test_" + System.currentTimeMillis() + ".txt";
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            
            log.info("S3 퍼블릭 업로드 테스트: fileName={}", fileName);
            
            // 퍼블릭으로 업로드
            S3UploadResponse response = s3Service.upload(
                bytes,
                fileName,
                S3Directory.TEMP
            );
            
            Map<String, String> result = new HashMap<>();
            result.put("s3Key", response.getS3Key());
            result.put("fileName", fileName);
            result.put("presignedUrl", response.getPresignedUrl());
            
            // 퍼블릭 URL 생성 (버킷이 퍼블릭이므로)
            String publicUrl = String.format(
                "https://%s.s3.%s.amazonaws.com/%s",
                "pullit-frontend-20250805",
                "ap-northeast-2",
                response.getS3Key()
            );
            result.put("publicUrl", publicUrl);
            
            log.info("퍼블릭 URL: {}", publicUrl);
            
            return ResponseEntity.ok(
                ApiResponse.success(result, "퍼블릭 업로드 성공 - URL로 직접 접근 가능")
            );
        } catch (Exception e) {
            log.error("S3 퍼블릭 업로드 실패", e);
            return ResponseEntity.ok(
                ApiResponse.error(ErrorCode.FILE_UPLOAD_FAILED)
            );
        }
    }
}
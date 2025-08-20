package com.pullit.common.file.controller;

import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.common.annotation.AuthUser;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.common.file.dto.request.FileUploadRequest;
import com.pullit.common.file.dto.response.FileDownloadResponse;
import com.pullit.common.file.dto.response.FileInfoResponse;
import com.pullit.common.file.dto.response.FileUploadResponse;
import com.pullit.common.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "File API", description = "파일 관리 API")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    
    private final FileService fileService;
    
    @Operation(summary = "파일 업로드", description = "파일을 S3에 업로드하고 메타데이터를 저장합니다")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @AuthUser CustomUserDetails currentUser,
            @Parameter(description = "업로드할 파일", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "파일 메타데이터")
            @ModelAttribute @Valid FileUploadRequest uploadRequest,
            HttpServletRequest request
    ) {
        log.info("파일 업로드 요청: fileName={}, size={}, userId={}, category={}", 
            file.getOriginalFilename(), file.getSize(), currentUser.getUserId(), 
            uploadRequest.getCategory());
        
        // 클라이언트 IP 설정
        uploadRequest.setClientIp(getClientIp(request));
        
        FileUploadResponse response = fileService.uploadFile(
            file, 
            uploadRequest, 
            currentUser.getUserId()
        );
        
        return ResponseEntity.ok(
            ApiResponse.success(response, "파일 업로드 성공")
        );
    }
    
    @Operation(summary = "파일 다운로드", description = "파일을 다운로드합니다")
    @GetMapping("/{fileId}/download")
    public ResponseEntity<byte[]> downloadFile(
            @AuthUser CustomUserDetails currentUser,
            @PathVariable Long fileId
    ) {
        log.info("파일 다운로드 요청: fileId={}, userId={}", fileId, currentUser.getUserId());
        
        Long userId = currentUser.getUserId();
        
        FileDownloadResponse response = fileService.downloadFile(fileId, userId);
        
        // 파일명 인코딩
        String encodedFileName = URLEncoder.encode(response.getOriginalFilename(), StandardCharsets.UTF_8)
            .replace("+", "%20");
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + response.getSafeFilename() + "\"; " +
                "filename*=UTF-8''" + encodedFileName)
            .header(HttpHeaders.CONTENT_TYPE, response.getContentType())
            .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(response.getFileSize()))
            .body(response.getData());
    }
    
    @Operation(summary = "Pre-signed URL 생성", description = "파일 다운로드용 임시 URL을 생성합니다")
    @GetMapping("/{fileId}/url")
    public ResponseEntity<ApiResponse<Map<String, String>>> getPresignedUrl(
            @AuthUser CustomUserDetails currentUser,
            @PathVariable Long fileId
    ) {
        log.info("Pre-signed URL 생성 요청: fileId={}, userId={}", fileId, currentUser.getUserId());
        
        Long userId = currentUser.getUserId();
        
        String url = fileService.generatePresignedUrl(fileId, userId);
        
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        result.put("expiresIn", "3600"); // 1시간
        
        return ResponseEntity.ok(
            ApiResponse.success(result, "Pre-signed URL 생성 성공")
        );
    }
    
    @Operation(summary = "파일 정보 조회", description = "파일 메타데이터를 조회합니다")
    @GetMapping("/{fileId}")
    public ResponseEntity<ApiResponse<FileInfoResponse>> getFileInfo(
            @AuthUser CustomUserDetails currentUser,
            @PathVariable Long fileId
    ) {
        log.info("파일 정보 조회: fileId={}, userId={}", fileId, currentUser.getUserId());
        
        Long userId = currentUser.getUserId();
        
        FileInfoResponse response = fileService.getFileInfo(fileId, userId);
        
        return ResponseEntity.ok(
            ApiResponse.success(response, "파일 정보 조회 성공")
        );
    }
    
    @Operation(summary = "사용자 파일 목록 조회", description = "사용자가 업로드한 파일 목록을 조회합니다")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<FileInfoResponse>>> getMyFiles(
            @AuthUser CustomUserDetails currentUser,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) 
            Pageable pageable
    ) {
        Long userId = currentUser.getUserId();
        
        Page<FileInfoResponse> response = fileService.getUserFiles(userId, pageable);
        
        return ResponseEntity.ok(
            ApiResponse.success(response, "파일 목록 조회 성공")
        );
    }
    
    @Operation(summary = "엔티티별 파일 조회", description = "특정 엔티티와 연결된 파일들을 조회합니다")
    @GetMapping("/entity")
    public ResponseEntity<ApiResponse<List<FileInfoResponse>>> getFilesByEntity(
            @RequestParam String entityType,
            @RequestParam Long entityId
    ) {
        log.info("엔티티별 파일 조회: entityType={}, entityId={}", entityType, entityId);
        
        List<FileInfoResponse> response = fileService.getFilesByEntity(entityType, entityId);
        
        return ResponseEntity.ok(
            ApiResponse.success(response, "파일 목록 조회 성공")
        );
    }
    
    @Operation(summary = "파일 검색", description = "파일명 또는 설명으로 파일을 검색합니다")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<FileInfoResponse>>> searchFiles(
            @RequestParam String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) 
            Pageable pageable
    ) {
        log.info("파일 검색: keyword={}", keyword);
        
        Page<FileInfoResponse> response = fileService.searchFiles(keyword, pageable);
        
        return ResponseEntity.ok(
            ApiResponse.success(response, "파일 검색 성공")
        );
    }
    
    @Operation(summary = "파일 삭제", description = "파일을 소프트 삭제합니다")
    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @AuthUser CustomUserDetails currentUser,
            @PathVariable Long fileId
    ) {
        log.info("파일 삭제 요청: fileId={}, userId={}", fileId, currentUser.getUserId());
        
        Long userId = currentUser.getUserId();
        
        fileService.deleteFile(fileId, userId);
        
        return ResponseEntity.ok(
            ApiResponse.success(null, "파일 삭제 성공")
        );
    }
    
    @Operation(summary = "파일 복원", description = "삭제된 파일을 복원합니다")
    @PostMapping("/{fileId}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreFile(
            @AuthUser CustomUserDetails currentUser,
            @PathVariable Long fileId
    ) {
        log.info("파일 복원 요청: fileId={}, userId={}", fileId, currentUser.getUserId());
        
        Long userId = currentUser.getUserId();
        
        fileService.restoreFile(fileId, userId);
        
        return ResponseEntity.ok(
            ApiResponse.success(null, "파일 복원 성공")
        );
    }
    
    @Operation(summary = "파일 영구 삭제", description = "파일을 영구적으로 삭제합니다")
    @DeleteMapping("/{fileId}/permanent")
    public ResponseEntity<ApiResponse<Void>> deleteFilePermanently(
            @AuthUser CustomUserDetails currentUser,
            @PathVariable Long fileId
    ) {
        log.info("파일 영구 삭제 요청: fileId={}, userId={}", fileId, currentUser.getUserId());
        
        Long userId = currentUser.getUserId();
        
        fileService.deleteFilePermanently(fileId, userId);
        
        return ResponseEntity.ok(
            ApiResponse.success(null, "파일 영구 삭제 성공")
        );
    }
    
    @Operation(summary = "여러 파일 삭제", description = "여러 파일을 한번에 삭제합니다")
    @DeleteMapping("/batch")
    public ResponseEntity<ApiResponse<Void>> deleteMultipleFiles(
            @AuthUser CustomUserDetails currentUser,
            @RequestBody List<Long> fileIds
    ) {
        log.info("여러 파일 삭제 요청: count={}, userId={}", fileIds.size(), currentUser.getUserId());
        
        Long userId = currentUser.getUserId();
        
        fileService.deleteMultipleFiles(fileIds, userId);
        
        return ResponseEntity.ok(
            ApiResponse.success(null, "파일 삭제 성공")
        );
    }
    
    @Operation(summary = "사용자 저장 용량 조회", description = "사용자의 총 파일 저장 용량을 조회합니다")
    @GetMapping("/storage/size")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserStorageSize(
            @AuthUser CustomUserDetails currentUser
    ) {
        Long userId = currentUser.getUserId();
        
        Long totalSize = fileService.getUserStorageSize(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalBytes", totalSize);
        result.put("totalMB", totalSize / (1024.0 * 1024.0));
        result.put("totalGB", totalSize / (1024.0 * 1024.0 * 1024.0));
        
        return ResponseEntity.ok(
            ApiResponse.success(result, "저장 용량 조회 성공")
        );
    }
    
    /**
     * 클라이언트 IP 추출
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 다중 프록시 환경에서 첫 번째 IP 추출
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
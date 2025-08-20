package com.pullit.common.file.service;

import com.pullit.common.file.dto.request.FileUploadRequest;
import com.pullit.common.file.dto.response.FileDownloadResponse;
import com.pullit.common.file.dto.response.FileInfoResponse;
import com.pullit.common.file.dto.response.FileUploadResponse;
import com.pullit.common.file.entity.FileMetadata;
import com.pullit.common.file.enums.FileCategory;
import com.pullit.common.file.exception.FileAccessDeniedException;
import com.pullit.common.file.exception.FileNotFoundException;
import com.pullit.common.file.repository.FileMetadataRepository;
import com.pullit.common.s3.config.S3Properties;
import com.pullit.common.s3.dto.S3UploadResponse;
import com.pullit.common.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FileService {
    
    private final S3Service s3Service;
    private final FileMetadataRepository fileRepository;
    private final S3Properties s3Properties;
    
    /**
     * 파일 업로드 (S3 + DB)
     */
    public FileUploadResponse uploadFile(
            MultipartFile file,
            FileUploadRequest request,
            Long userId
    ) {
        log.info("파일 업로드 시작: fileName={}, size={}, userId={}", 
            file.getOriginalFilename(), file.getSize(), userId);
        
        // 1. S3 업로드
        S3UploadResponse s3Response = s3Service.upload(
            file,
            request.getDirectory()
        );
        
        // 2. 파일 확장자 추출
        String extension = "";
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        // 3. 카테고리 자동 설정
        FileCategory category = request.getCategory();
        if (category == null) {
            category = FileCategory.fromContentType(file.getContentType());
        }
        
        // 4. 만료 시간 설정
        LocalDateTime expiresAt = null;
        if (request.getExpirationHours() != null && request.getExpirationHours() > 0) {
            expiresAt = LocalDateTime.now().plusHours(request.getExpirationHours());
        }
        
        // 5. DB 저장
        FileMetadata fileMetadata = FileMetadata.builder()
            .s3Key(s3Response.getS3Key())
            .bucketName(s3Properties.getBucketName())
            .originalFilename(originalFilename)
            .fileSize(file.getSize())
            .contentType(file.getContentType())
            .fileExtension(extension)
            .fileCategory(category)
            .directoryType(request.getDirectory())
            .uploadedBy(userId)
            .uploadIp(request.getClientIp())
            .entityType(request.getEntityType())
            .entityId(request.getEntityId())
            .description(request.getDescription())
            .tags(request.getTags())
            .metadata(request.getMetadata())
            .isPublic(request.isPublic())
            .expiresAt(expiresAt)
            .build();
            
        fileRepository.save(fileMetadata);
        
        log.info("파일 업로드 완료: fileId={}, s3Key={}", 
            fileMetadata.getId(), s3Response.getS3Key());
        
        return FileUploadResponse.from(fileMetadata, s3Response);
    }
    
    /**
     * 파일 다운로드
     */
    @Transactional(readOnly = true)
    public FileDownloadResponse downloadFile(Long fileId, Long userId) {
        log.info("파일 다운로드 요청: fileId={}, userId={}", fileId, userId);
        
        // 1. DB 조회
        FileMetadata file = fileRepository.findById(fileId)
            .orElseThrow(() -> new FileNotFoundException(fileId));
        
        // 2. 삭제된 파일 체크
        if (file.getIsDeleted()) {
            throw new FileNotFoundException("삭제된 파일입니다.");
        }
        
        // 3. 권한 체크
        if (!file.canAccess(userId)) {
            throw new FileAccessDeniedException(fileId, userId);
        }
        
        // 4. S3 다운로드
        byte[] data = s3Service.download(file.getS3Key());
        
        log.info("파일 다운로드 성공: fileId={}, size={} bytes", fileId, data.length);
        
        return FileDownloadResponse.builder()
            .data(data)
            .originalFilename(file.getOriginalFilename())
            .contentType(file.getContentType())
            .fileSize(file.getFileSize())
            .build();
    }
    
    /**
     * Pre-signed URL 생성
     */
    @Transactional(readOnly = true)
    public String generatePresignedUrl(Long fileId, Long userId) {
        FileMetadata file = fileRepository.findById(fileId)
            .orElseThrow(() -> new FileNotFoundException(fileId));
        
        if (!file.canAccess(userId)) {
            throw new FileAccessDeniedException(fileId, userId);
        }
        
        // 기본 1시간 유효
        return s3Service.generatePresignedUrl(
            file.getS3Key(), 
            Duration.ofHours(1)
        );
    }
    
    /**
     * 파일 정보 조회
     */
    @Transactional(readOnly = true)
    public FileInfoResponse getFileInfo(Long fileId, Long userId) {
        FileMetadata file = fileRepository.findById(fileId)
            .orElseThrow(() -> new FileNotFoundException(fileId));
        
        if (!file.canAccess(userId)) {
            throw new FileAccessDeniedException(fileId, userId);
        }
        
        FileInfoResponse response = FileInfoResponse.from(file);
        
        // Pre-signed URL 추가 (선택적)
        if (!file.getIsDeleted()) {
            String url = s3Service.generatePresignedUrl(file.getS3Key());
            response.withDownloadUrl(url);
        }
        
        return response;
    }
    
    /**
     * 파일 목록 조회 (사용자별)
     */
    @Transactional(readOnly = true)
    public Page<FileInfoResponse> getUserFiles(Long userId, Pageable pageable) {
        Page<FileMetadata> files = fileRepository.findByUploadedByAndIsDeletedFalse(userId, pageable);
        
        return files.map(FileInfoResponse::from);
    }
    
    /**
     * 엔티티별 파일 조회
     */
    @Transactional(readOnly = true)
    public List<FileInfoResponse> getFilesByEntity(String entityType, Long entityId) {
        List<FileMetadata> files = fileRepository.findByEntityTypeAndEntityIdAndIsDeletedFalse(
            entityType, entityId
        );
        
        return files.stream()
            .map(FileInfoResponse::from)
            .collect(Collectors.toList());
    }
    
    /**
     * 파일 검색
     */
    @Transactional(readOnly = true)
    public Page<FileInfoResponse> searchFiles(String keyword, Pageable pageable) {
        Page<FileMetadata> files = fileRepository.searchByKeyword(keyword, pageable);
        
        return files.map(FileInfoResponse::from);
    }
    
    /**
     * 파일 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteFile(Long fileId, Long userId) {
        FileMetadata file = fileRepository.findById(fileId)
            .orElseThrow(() -> new FileNotFoundException(fileId));
        
        // 권한 체크 (업로더만 삭제 가능)
        if (!file.getUploadedBy().equals(userId)) {
            throw new FileAccessDeniedException("파일 삭제 권한이 없습니다.");
        }
        
        // 소프트 삭제
        file.softDelete();
        
        log.info("파일 소프트 삭제: fileId={}, s3Key={}", fileId, file.getS3Key());
    }
    
    /**
     * 파일 복원
     */
    @Transactional
    public void restoreFile(Long fileId, Long userId) {
        FileMetadata file = fileRepository.findById(fileId)
            .orElseThrow(() -> new FileNotFoundException(fileId));
        
        // 권한 체크
        if (!file.getUploadedBy().equals(userId)) {
            throw new FileAccessDeniedException("파일 복원 권한이 없습니다.");
        }
        
        // 복원
        file.restore();
        
        log.info("파일 복원: fileId={}, s3Key={}", fileId, file.getS3Key());
    }
    
    /**
     * 파일 영구 삭제
     */
    @Transactional
    public void deleteFilePermanently(Long fileId, Long userId) {
        FileMetadata file = fileRepository.findById(fileId)
            .orElseThrow(() -> new FileNotFoundException(fileId));
        
        // 권한 체크
        if (!file.getUploadedBy().equals(userId)) {
            throw new FileAccessDeniedException("파일 삭제 권한이 없습니다.");
        }
        
        // S3에서 삭제
        s3Service.delete(file.getS3Key());
        
        // DB에서 삭제
        fileRepository.delete(file);
        
        log.info("파일 영구 삭제: fileId={}, s3Key={}", fileId, file.getS3Key());
    }
    
    /**
     * 여러 파일 삭제
     */
    @Transactional
    public void deleteMultipleFiles(List<Long> fileIds, Long userId) {
        List<FileMetadata> files = fileRepository.findByIdInAndIsDeletedFalse(fileIds);
        
        for (FileMetadata file : files) {
            // 권한 체크
            if (file.getUploadedBy().equals(userId)) {
                file.softDelete();
                log.info("파일 소프트 삭제: fileId={}", file.getId());
            }
        }
    }
    
    /**
     * 만료된 파일 정리 (스케줄러)
     * 매일 새벽 2시 실행
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredFiles() {
        log.info("만료된 파일 정리 시작");
        
        LocalDateTime now = LocalDateTime.now();
        List<FileMetadata> expiredFiles = fileRepository.findByExpiresAtBeforeAndIsDeletedTrue(now);
        
        int deletedCount = 0;
        for (FileMetadata file : expiredFiles) {
            try {
                // S3에서 실제 삭제
                s3Service.delete(file.getS3Key());
                
                // DB에서 삭제
                fileRepository.delete(file);
                
                deletedCount++;
                log.debug("만료 파일 삭제: fileId={}, s3Key={}", file.getId(), file.getS3Key());
                
            } catch (Exception e) {
                log.error("파일 삭제 실패: fileId={}, error={}", file.getId(), e.getMessage());
            }
        }
        
        log.info("만료된 파일 정리 완료: {}개 파일 삭제", deletedCount);
    }
    
    /**
     * 임시 파일 정리 (스케줄러)
     * 매일 새벽 3시 실행
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupTempFiles() {
        log.info("임시 파일 정리 시작");
        
        // S3 임시 파일 정리 (7일 이상 된 파일)
        s3Service.cleanupTempFiles(7);
        
        log.info("임시 파일 정리 완료");
    }
    
    /**
     * 사용자 저장 용량 조회
     */
    @Transactional(readOnly = true)
    public Long getUserStorageSize(Long userId) {
        Long totalSize = fileRepository.getTotalFileSizeByUser(userId);
        return totalSize != null ? totalSize : 0L;
    }
}
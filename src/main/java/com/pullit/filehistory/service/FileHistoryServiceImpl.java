package com.pullit.filehistory.service;

import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;
import com.pullit.common.file.entity.FileMetadata;
import com.pullit.common.file.repository.FileMetadataRepository;
import com.pullit.common.s3.enums.S3Directory;
import com.pullit.common.s3.service.S3Service;
import com.pullit.common.s3.dto.S3UploadRequest;
import com.pullit.filehistory.dto.FileHistoryDTO;
import com.pullit.filehistory.dto.PdfImageDTO;
import com.pullit.filehistory.entity.FileHistory;
import com.pullit.filehistory.entity.PdfImage;
import com.pullit.filehistory.repository.FileHistoryRepository;
import com.pullit.filehistory.repository.PdfImageRepository;
import com.pullit.item.entity.Subject;
import com.pullit.itemprocess.entity.ProcessedItem;
import com.pullit.itemprocess.repository.ProcessedItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Transactional
@Slf4j
@Service
@RequiredArgsConstructor
public class FileHistoryServiceImpl implements FileHistoryService {
    private final FileHistoryRepository fileHistoryRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final PdfImageRepository pdfImageRepository;
    private final ProcessedItemRepository processedItemRepository;
    private final S3Service s3Service;

    @Override
    public Long createHistory(Long fileMetadataId, Long subjectId, CustomUserDetails currentUser) {
        log.debug("createHistory: fileMetadataId={}, subjectId={}", fileMetadataId, subjectId);

        FileMetadata fileMetadata = fileMetadataRepository.findById(fileMetadataId).orElseThrow(() -> new BusinessException(ErrorCode.INVALID_BUSINESS_LOGIC));

        FileHistory fileHistory = FileHistory.builder()
                .fileMetadata(fileMetadata)
                .subject(Subject.builder().subjectId(subjectId).build())
                .fileHistoryName(fileMetadata.getOriginalFilename())
                .imgCount(0)
                .imgOrder("")
                .createdBy(currentUser.getUsername())
                .build();

        if (fileHistory != null) {
            fileHistoryRepository.save(fileHistory);
            return fileHistory.getId();
        } else {
            throw new BusinessException(ErrorCode.INVALID_BUSINESS_LOGIC);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<FileHistoryDTO> getFileHistories(Pageable pageable, String areaCode, CustomUserDetails currentUser) {
        log.debug("getFileHistories: areaCode={}, user={}", areaCode, currentUser.getUsername());
        
        Page<FileHistory> fileHistories = fileHistoryRepository.findByCreatedByOrderByCreatedDateDesc(
            currentUser.getUsername(), pageable);
        
        // Page 객체를 DTO로 변환 (areaCode 필터링은 프론트엔드에서 처리)
        return fileHistories.map(FileHistoryDTO::from);
    }

    @Override
    public FileHistoryDTO getFileHistory(Long fileHistoryId, CustomUserDetails currentUser) {
        log.debug("getFileHistory: fileHistoryId={}, user={}", fileHistoryId, currentUser.getUsername());

        FileHistory fileHistory = fileHistoryRepository.findById(fileHistoryId).orElse(null);
        if (fileHistory == null) {
            throw new BusinessException(ErrorCode.INVALID_BUSINESS_LOGIC);
        }

        return FileHistoryDTO.from(fileHistory);
    }
    // 0-based 퍼뮤테이션 검증/파싱: 길이 n, 값 0..n-1, 중복 없음이면 그대로 반환, 아니면 null
    private List<Integer> parseZeroBasedPermutation(String raw, int n) {
        if (raw == null) return null;
        String[] parts = raw.split(",");
        if (parts.length != n) return null;

        boolean[] seen = new boolean[n];
        List<Integer> result = new ArrayList<>(n);
        for (String s : parts) {
            s = s.trim();
            if (s.isEmpty()) return null;
            int v;
            try { v = Integer.parseInt(s); } catch (Exception e) { return null; }
            if (v < 0 || v >= n || seen[v]) return null;
            seen[v] = true;
            result.add(v);
        }
        return result;
    }
    private List<PdfImage> reorderByImgOrder(List<PdfImage> source, String imgOrder) {
        if (source == null || source.isEmpty()) return source;

        // pageNumber 오름차순이 '원본 인덱스' 0..n-1에 대응
        List<PdfImage> byPage = new ArrayList<>(source);
        byPage.sort(Comparator.comparing(PdfImage::getPageNumber));

        List<Integer> perm = parseZeroBasedPermutation(imgOrder, byPage.size());
        if (perm == null) {
            // 퍼뮤테이션이 아니면 원본 순서 유지(또는 여기서 예외 던져도 됨)
            log.warn("imgOrder is not a valid zero-based permutation. order={}", imgOrder);
            return byPage;
        }
        return perm.stream().map(byPage::get).collect(Collectors.toList());
    }
    @Override
    @Transactional(readOnly = true)
    public List<PdfImageDTO> getFileHistoryImages(Long fileHistoryId, CustomUserDetails currentUser) {
        log.debug("getFileHistoryImages: fileHistoryId={}, user={}", fileHistoryId, currentUser.getUsername());

        // 자신 소유의 히스토리만 조회
        FileHistory fileHistory = fileHistoryRepository.findByIdAndCreatedBy(fileHistoryId, currentUser.getUsername());
        if (fileHistory == null) {
            throw new BusinessException(ErrorCode.INVALID_BUSINESS_LOGIC);
        }

        // 원본은 pageNumber 오름차순으로 가져옴
        List<PdfImage> images = pdfImageRepository.findByFileHistoryOrderByPageNumber(fileHistory);
        if (images == null || images.isEmpty()) {
            return List.of();
        }

        // imgOrder 적용해 표시 순서 재정렬
        List<PdfImage> ordered = reorderByImgOrder(images, fileHistory.getImgOrder());

        // 조회 시점마다 presigned 재발급(1시간)해서 DTO 구성
        return ordered.stream()
                .map(img -> {
                    String freshUrl = s3Service.generatePresignedUrl(img.getS3Key(), Duration.ofHours(1));
                    PdfImageDTO dto = PdfImageDTO.from(img);
                    dto.setImageUrl(freshUrl);
                    // (선택) FE 디버깅 편의: pageNumber 그대로 전달(0-based 필요시 FE에서 -1 사용)
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public String uploadTinyMceImage(MultipartFile file, CustomUserDetails currentUser) {
        log.info("TinyMCE image upload: fileName={}, size={}, userId={}", 
                file.getOriginalFilename(), file.getSize(), currentUser.getUserId());
        
        // 1. 파일 검증
        validateImageFile(file);
        
        // 2. S3 업로드 (권한 검증은 인증된 사용자만 접근 가능하므로 불필요)
        try {
            String originalFilename = file.getOriginalFilename();
            String fileName = String.format("tinymce_%d_%s", currentUser.getUserId(), 
                    originalFilename != null ? originalFilename : "image.jpg");
            
            // TinyMCE 이미지는 Public으로 업로드 (영구 URL 사용)
            S3UploadRequest request = S3UploadRequest.builder()
                    .fileData(file.getBytes())
                    .fileName(fileName)
                    .directory(S3Directory.IMAGE_QUESTION)
                    .publicRead(true)  // Public 읽기 권한 설정
                    .build();
            var uploadResponse = s3Service.upload(request);
            String s3Key = uploadResponse.getS3Key();
            String s3Url = uploadResponse.getPublicUrl();

            // 4. FileMetadata 생성 (TinyMCE 업로드용, FileHistory 없이)
            String extension = originalFilename != null && originalFilename.contains(".") 
                    ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
            
            FileMetadata fileMetadata = FileMetadata.builder()
                    .originalFilename(originalFilename)
                    .s3Key(s3Key)
                    .s3Url(s3Url)
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .fileExtension(extension)
                    .directoryType(S3Directory.IMAGE_QUESTION)
                    .uploadedBy(currentUser.getUserId())
                    .entityType("ProcessedItem")
                    .entityId(0L)
                    .isPublic(true)  // Public 업로드로 변경
                    .build();

            fileMetadataRepository.save(fileMetadata);

            // 5. 깔끔한 Public URL 반환 (확장자 뒤 쿼리 제거)
//            String cleanPublicUrl = cleanImageUrl(s3Url);
            
            log.info("TinyMCE image upload completed: s3Key={}, publicUrl={}", s3Key, s3Url);
            return s3Url;
            
        } catch (Exception e) {
            log.error("TinyMCE image upload failed: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }
    
    private void validateImageFile(MultipartFile file) {
        // MIME 타입 검증
        List<String> allowedTypes = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/svg+xml");
        if (!allowedTypes.contains(file.getContentType())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        
        // 파일 크기 검증 (5MB)
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        
        // 빈 파일 검증
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
    
//    private String cleanImageUrl(String s3Url) {
//        if (s3Url == null) return null;
//
//        // 확장자 패턴 매칭: .jpg, .png, .svg, .gif, .webp 등
//        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^(.*\\.(jpg|jpeg|png|gif|webp|svg))",
//                java.util.regex.Pattern.CASE_INSENSITIVE);
//        java.util.regex.Matcher matcher = pattern.matcher(s3Url);
//
//        return matcher.find() ? matcher.group(1) : s3Url;
//    }
}

package com.pullit.filehistory.service;

import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;
import com.pullit.common.file.entity.FileMetadata;
import com.pullit.common.file.repository.FileMetadataRepository;
import com.pullit.filehistory.dto.FileHistoryDTO;
import com.pullit.filehistory.dto.PdfImageDTO;
import com.pullit.filehistory.entity.FileHistory;
import com.pullit.filehistory.repository.FileHistoryRepository;
import com.pullit.item.entity.Subject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Transactional
@Slf4j
@Service
@RequiredArgsConstructor
public class FileHistoryServiceImpl implements FileHistoryService {
    private final FileHistoryRepository fileHistoryRepository;
    private final FileMetadataRepository fileMetadataRepository;

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
    @Transactional(readOnly = true)
    public List<PdfImageDTO> getFileHistoryImages(Long fileHistoryId, CustomUserDetails currentUser) {
        log.debug("getFileHistoryImages: fileHistoryId={}, user={}", fileHistoryId, currentUser.getUsername());
        
        FileHistory fileHistory = fileHistoryRepository.findByIdAndCreatedBy(fileHistoryId, currentUser.getUsername());
        if (fileHistory == null) {
            throw new BusinessException(ErrorCode.INVALID_BUSINESS_LOGIC);
        }
        
        return fileHistory.getPdfImages() != null ? 
            fileHistory.getPdfImages().stream()
                .map(PdfImageDTO::from)
                .collect(Collectors.toList()) : 
            List.of();
    }
}

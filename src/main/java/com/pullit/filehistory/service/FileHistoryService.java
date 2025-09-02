package com.pullit.filehistory.service;

import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.filehistory.dto.FileHistoryDTO;
import com.pullit.filehistory.dto.PdfImageDTO;
import com.pullit.filehistory.entity.FileHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileHistoryService {
    Long createHistory(Long fileMetadataId, Long subjectId, CustomUserDetails currentUser);
    
    Page<FileHistoryDTO> getFileHistories(Pageable pageable, String areaCode, CustomUserDetails currentUser);
    
    List<PdfImageDTO> getFileHistoryImages(Long fileHistoryId, CustomUserDetails currentUser);

    FileHistoryDTO getFileHistory(Long fileHistoryId, CustomUserDetails currentUser);
    
    String uploadTinyMceImage(MultipartFile file, CustomUserDetails currentUser);
}

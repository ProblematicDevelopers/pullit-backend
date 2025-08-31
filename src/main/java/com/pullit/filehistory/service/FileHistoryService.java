package com.pullit.filehistory.service;

import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.filehistory.dto.FileHistoryDTO;
import com.pullit.filehistory.dto.PdfImageDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FileHistoryService {
    Long createHistory(Long fileMetadataId, Long subjectId, CustomUserDetails currentUser);
    
    Page<FileHistoryDTO> getFileHistories(Pageable pageable, String areaCode, CustomUserDetails currentUser);
    
    List<PdfImageDTO> getFileHistoryImages(Long fileHistoryId, CustomUserDetails currentUser);
}

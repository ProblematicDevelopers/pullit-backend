package com.pullit.filehistory.service;

import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.common.annotation.AuthUser;
import org.springframework.web.bind.annotation.RequestParam;

public interface FileHistoryService {
    public Long createHistory(Long fileMetadataId, Long subjectId, CustomUserDetails currentUser);
}

package com.pullit.common.file.exception;

import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;

public class FileAccessDeniedException extends BusinessException {
    
    public FileAccessDeniedException() {
        super(ErrorCode.ACCESS_DENIED);
    }
    
    public FileAccessDeniedException(String message) {
        super(ErrorCode.ACCESS_DENIED, message);
    }
    
    public FileAccessDeniedException(Long fileId, Long userId) {
        super(ErrorCode.ACCESS_DENIED, 
            String.format("파일 접근 권한이 없습니다. (파일 ID: %d, 사용자 ID: %d)", fileId, userId));
    }
}
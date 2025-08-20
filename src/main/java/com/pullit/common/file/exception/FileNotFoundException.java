package com.pullit.common.file.exception;

import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;

public class FileNotFoundException extends BusinessException {
    
    public FileNotFoundException() {
        super(ErrorCode.FILE_NOT_FOUND);
    }
    
    public FileNotFoundException(String message) {
        super(ErrorCode.FILE_NOT_FOUND, message);
    }
    
    public FileNotFoundException(Long fileId) {
        super(ErrorCode.FILE_NOT_FOUND, 
            String.format("파일을 찾을 수 없습니다. (ID: %d)", fileId));
    }
}
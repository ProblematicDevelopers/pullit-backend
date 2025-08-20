package com.pullit.common.file.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileDownloadResponse {
    
    private byte[] data;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    
    /**
     * 안전한 파일명 생성 (ASCII만)
     */
    public String getSafeFilename() {
        if (isAsciiPrintable(originalFilename)) {
            return originalFilename;
        }
        
        // 확장자 추출
        String extension = "";
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot > 0) {
            extension = originalFilename.substring(lastDot);
        }
        
        return "download" + extension;
    }
    
    private boolean isAsciiPrintable(String str) {
        if (str == null) return false;
        for (char c : str.toCharArray()) {
            if (c < 32 || c > 126) {
                return false;
            }
        }
        return true;
    }
}
package com.pullit.filehistory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfProcessingResponse {
    
    /**
     * 파일 히스토리 ID
     */
    private Long fileHistoryId;
    
    /**
     * 총 페이지 수
     */
    private int totalPages;
    
    /**
     * PDF 이미지 정보 목록
     */
    private List<PdfImageInfo> images;
    
    /**
     * 처리 상태 메시지
     */
    private String message;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PdfImageInfo {
        /**
         * PDF 이미지 ID
         */
        private Long pdfImageId;
        
        /**
         * 페이지 번호 (1부터 시작)
         */
        private int pageNumber;
        
        /**
         * 이미지 URL (S3 presigned URL)
         */
        private String imageUrl;
        
        /**
         * 이미지 너비 (픽셀)
         */
        private int width;
        
        /**
         * 이미지 높이 (픽셀)
         */
        private int height;
        
        /**
         * 이미지 파일 크기 (바이트)
         */
        private long fileSize;
    }
}
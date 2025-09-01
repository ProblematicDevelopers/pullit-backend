package com.pullit.filehistory.dto.request;

import com.pullit.itemprocess.enums.AreaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrHistoryBulkSaveRequest {
    
    private Long processedItemId;
    private Long pdfImageId;
    private List<AreaData> areas;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AreaData {
        private AreaType areaType;
        private Integer pageNo;
        
        // 정규화 좌표 (0~1)
        private Double x;
        private Double y;
        private Double width;
        private Double height;
        
        // 렌더 컨텍스트
        private Double scale;
        private Double rotation;
        private Integer canvasWidth;
        private Integer canvasHeight;
        
        // 원본 픽셀 좌표
        private Double pixelX;
        private Double pixelY;
        private Double pixelWidth;
        private Double pixelHeight;
        
        // 이미지 URL (선택사항)
        private String originalImageUrl;
        
        // OCR 텍스트 (있다면)
        private String ocrText;
    }
}
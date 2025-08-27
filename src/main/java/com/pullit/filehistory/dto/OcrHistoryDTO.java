package com.pullit.filehistory.dto;

import com.pullit.filehistory.entity.OcrHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrHistoryDTO {
    private Long id;
    private Long pdfImageId;
    private Long itemId;
    private String positionX;;
    private String positionY;
    private String sizeX;
    private String sizeY;

    public static OcrHistoryDTO from(OcrHistory ocrHistory) {
        if (ocrHistory == null) {
            return null;
        }
        return OcrHistoryDTO.builder()
                .id(ocrHistory.getId())
                .pdfImageId(ocrHistory.getPdfImage() != null ? ocrHistory.getPdfImage().getId() : null)
                .itemId(ocrHistory.getItemMetadata() != null ? ocrHistory.getItemMetadata().getItemId() : null)
                .sizeX(ocrHistory.getSizeX())
                .sizeY(ocrHistory.getSizeY())
                .build();
    }
}

package com.pullit.filehistory.dto;

import com.pullit.filehistory.entity.PdfImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdfImageDTO {
    private Long id;
    private Long fileHistoryId;
    private Integer pageNumber;
    private String imageUrl;
    private Integer imageWidth;
    private Integer imageHeight;
    private Long fileSize;
    private String s3Key;
    private List<OcrHistoryDTO> ocrHistories;

    public static PdfImageDTO from(PdfImage pdfImage) {
        if (pdfImage == null) {
            return null;
        }
        return PdfImageDTO.builder()
                .id(pdfImage.getId())
                .fileHistoryId(pdfImage.getFileHistory() != null ? pdfImage.getFileHistory().getId() : null)
                .pageNumber(pdfImage.getPageNumber())
                .imageUrl(pdfImage.getImageUrl())
                .imageWidth(pdfImage.getImageWidth())
                .imageHeight(pdfImage.getImageHeight())
                .fileSize(pdfImage.getFileSize())
                .s3Key(pdfImage.getS3Key())
                .ocrHistories(pdfImage.getOcrHistories() != null ? pdfImage.getOcrHistories().stream().map(OcrHistoryDTO::from).collect(Collectors.toList()) : null)
                .build();
    }
}


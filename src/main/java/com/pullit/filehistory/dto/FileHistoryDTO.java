package com.pullit.filehistory.dto;

import com.pullit.common.file.entity.FileMetadata;
import com.pullit.filehistory.entity.FileHistory;
import com.pullit.item.entity.Subject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileHistoryDTO {
    private Long id;
    private Long fileMetadataId;
    private Long subjectId;
    private String fileHistoryName;
    private String originalFileName;
    private String areaCode;
    private Long fileSize;
    private String status;
    private int imgCount;
    //e.g) "1,2,3,4,5"
    private String imgOrder;
    private String createdBy;
    private List<PdfImageDTO> pdfImages;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    public static FileHistoryDTO from(FileHistory fileHistory) {
        if (fileHistory == null) {
            return null;
        }
        return FileHistoryDTO.builder()
                .id(fileHistory.getId())
                .fileMetadataId(fileHistory.getFileMetadata() != null ? fileHistory.getFileMetadata().getId() : null)
                .subjectId(fileHistory.getSubject() != null ? fileHistory.getSubject().getSubjectId() : null)
                .fileHistoryName(fileHistory.getFileHistoryName())
                .originalFileName(fileHistory.getFileMetadata() != null ? fileHistory.getFileMetadata().getOriginalFilename() : null)
                .areaCode(fileHistory.getSubject() != null ? fileHistory.getSubject().getArea() != null ? fileHistory.getSubject().getArea().getCode() : null : null)
                .fileSize(fileHistory.getFileMetadata() != null ? fileHistory.getFileMetadata().getFileSize() : null)
                .status("completed") // 기본값으로 설정, 추후 상태 관리 로직 필요시 수정
                .imgCount(fileHistory.getImgCount())
                .imgOrder(fileHistory.getImgOrder())
                .pdfImages(fileHistory.getPdfImages() != null ? fileHistory.getPdfImages().stream().map(PdfImageDTO::from).collect(Collectors.toList()) : null)
                .createdBy(fileHistory.getCreatedBy())
                .createdDate(fileHistory.getCreatedDate())
                .updatedDate(fileHistory.getUpdatedDate())
                .build();
    }

    public FileHistory toEntity(FileMetadata fileMetadata, Subject subject) {
        if (fileMetadata == null && subject == null) {
            return null;
        }
        return FileHistory.builder()
                .id(this.getId())
                .fileMetadata(fileMetadata)
                .subject(subject)
                .fileHistoryName(this.getFileHistoryName())
                .createdBy(this.getCreatedBy())
                .build();
    }
}

package com.pullit.filehistory.dto;

import com.pullit.filehistory.entity.FileHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.sql.Date;
import java.sql.Timestamp;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileHistoryDTO {
    private Long id;

    private Long subjectId;

    private String fileHistoryName;

    private String originPdfUrl;

    private String createdBy;

    private Timestamp createdDate;

    public static FileHistoryDTO from(FileHistory fileHistory) {
        if (fileHistory == null) {
            return null;
        }
        return FileHistoryDTO.builder()
                .id(fileHistory.getId())
                .subjectId(fileHistory.getSubjectId())
                .fileHistoryName(fileHistory.getFileHistoryName())
                .createdBy(fileHistory.getCreatedBy())
                .createdDate(fileHistory.getCreatedDate())
                .build();
    }
}

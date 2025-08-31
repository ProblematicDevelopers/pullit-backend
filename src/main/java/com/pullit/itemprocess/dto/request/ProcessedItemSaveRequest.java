package com.pullit.itemprocess.dto.request;

import com.pullit.itemprocess.enums.AreaType;
import com.pullit.itemprocess.enums.DifficultyLevel;
import com.pullit.itemprocess.enums.ItemType;
import lombok.Data;

import java.util.List;

@Data
public class ProcessedItemSaveRequest {
    private ItemType type;
    private DifficultyLevel difficulty;
    private String answer;
    private Integer score;
    private Long majorChapterId;
    private Long middleChapterId;
    private Long minorChapterId;
    private String solution;
    private String explanation;
    private Long passageId;
    
    private List<OcrHistoryData> ocrHistories;
    
    @Data
    public static class OcrHistoryData {
        private Long pdfImageId;
        private AreaType areaType;
        private String ocrText;
        private String editedText;
        private String originalImageUrl;
        private String positionX;
        private String positionY;
        private String sizeX;
        private String sizeY;
    }
}
package com.pullit.itemprocess.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.pullit.item.embedded.ChapterHierarchy;
import com.pullit.item.embedded.CodeNamePair;
import com.pullit.item.entity.Subject;
import com.pullit.itemprocess.entity.ProcessItemHtmlData;
import com.pullit.itemprocess.entity.ProcessItemImageData;
import com.pullit.itemprocess.entity.ProcessItemMetadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessItemMetadataResponse {
    private Long itemId;
    private Long sourceItemId;
    private Subject subject;
    private CodeNamePair questionForm;
    private CodeNamePair difficulty;
    private ChapterHierarchy chapterHierarchy;
    private Long passageId;
    private Boolean hasHtmlData;
    private Boolean hasImageData;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    
    // HTML 데이터 (선택적으로 포함)
    private ProcessItemHtmlData htmlData;
    
    // 이미지 데이터 (선택적으로 포함)
    private ProcessItemImageData imageData;
    
    // 지문 관련 정보
    private String passageContent;
    private String passageHtml;
    private String questionContent;
    private String questionHtml;
    private String answerContent;
    private String answerHtml;
    private String explainContent;
    private String explainHtml;
    
    // 선택지 정보
    private List<String> choices;
    private String choice1Html;
    private String choice2Html;
    private String choice3Html;
    private String choice4Html;
    private String choice5Html;
    
    // 통계 정보
    private boolean isPassageGroup;
    private int passageGroupSize; // 같은 지문 그룹의 문항 수
    
    // 편의 메서드
    public Long getSubjectId() {
        return subject != null ? subject.getSubjectId() : null;
    }
    
    public String getSubjectName() {
        return subject != null ? subject.getSubjectName() : null;
    }
}

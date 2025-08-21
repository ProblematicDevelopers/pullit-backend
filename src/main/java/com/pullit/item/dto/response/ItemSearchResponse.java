package com.pullit.item.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pullit.item.embedded.ChapterHierarchy;
import com.pullit.item.embedded.CodeNamePair;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItemSearchResponse {

    private Long itemId;
    private Long subjectId;
    private String subjectName;

    private Boolean hasImageData;
    private Boolean hasHtmlData;

    private String questionImageUrl;
    private String answerImageUrl;
    private String explainImageUrl;
    private String passageImageUrl;

    private String questionHtml;
    private String answerHtml;
    private String explainHtml;
    private String passageHtml;

    // 선택지 HTML 필드들 (CBT Report 전용)
    private String choice1Html;
    private String choice2Html;
    private String choice3Html;
    private String choice4Html;
    private String choice5Html;

    private CodeNamePair questionForm;
    private CodeNamePair difficulty;
    private ChapterHierarchy chapterHierarchy;

    private Long passageId;

    @Builder.Default
    private Integer points = 5;

    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    // === 프론트엔드 호환용 메서드들 ===

    // Step2ItemSelection.vue에서 사용하는 필드명 매핑
    public String getQuestionUrl() {
        return questionImageUrl;
    }

    // 챕터명 표시용
    public String getChapterName() {
        if (chapterHierarchy != null) {
            // 대단원명만 표시
            return chapterHierarchy.getLargeChapter().getName();

        }
        return "단원 정보 없음";
    }
    // 전체 챕터 경로
    public String getChapterFullPath() {
        if (chapterHierarchy != null) {
            return chapterHierarchy.getFullPath();
        }
        return "";
    }

    // 이미지 존재 여부
    public boolean hasAnyImage() {
        return hasImageData != null && hasImageData;
    }

    // 메인 이미지 URL 가져오기
    public String getMainImageUrl() {
        if (questionImageUrl != null) return questionImageUrl;
        if (passageImageUrl != null) return passageImageUrl;
        if (answerImageUrl != null) return answerImageUrl;
        if (explainImageUrl != null) return explainImageUrl;
        return null;
    }

    // === 정적 팩토리 메서드 ===

    public static ItemSearchResponse from(
            com.pullit.item.entity.ItemMetadata metadata,
            com.pullit.item.entity.ItemImageData imageData,
            com.pullit.item.entity.ItemHtmlData htmlData) {

        ItemSearchResponseBuilder builder = ItemSearchResponse.builder()
                .itemId(metadata.getItemId())
                .subjectId(metadata.getSubject() != null ? metadata.getSubject().getSubjectId() : null)
                .subjectName(metadata.getSubject() != null ? metadata.getSubject().getSubjectName() : null)
                .hasImageData(metadata.getHasImageData())
                .hasHtmlData(metadata.getHasHtmlData())
                .questionForm(metadata.getQuestionForm())
                .difficulty(metadata.getDifficulty())
                .chapterHierarchy(metadata.getChapterHierarchy())
                .passageId(metadata.getPassageId())
                .createdDate(metadata.getCreatedDate())
                .updatedDate(metadata.getUpdatedDate());

        // 이미지 데이터 매핑
        if (imageData != null) {
            builder.questionImageUrl(imageData.getQuestionUrl())
                    .answerImageUrl(imageData.getAnswerUrl())
                    .explainImageUrl(imageData.getExplainUrl())
                    .passageImageUrl(imageData.getPassageUrl());
        }

        // HTML 데이터 매핑 (필요시)
        if (htmlData != null) {
            builder.questionHtml(htmlData.getQuestionHtml())
                    .answerHtml(htmlData.getAnswerHtml())
                    .explainHtml(htmlData.getExplainHtml())
                    .passageHtml(htmlData.getPassageHtml());
        }

        return builder.build();
    }
}

package com.pullit.itemprocess.service;

import com.pullit.chapter.repository.ChapterRepository;
import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;
import com.pullit.filehistory.entity.FileHistory;
import com.pullit.filehistory.entity.OcrHistory;
import com.pullit.filehistory.entity.PdfImage;
import com.pullit.filehistory.repository.OcrHistoryRepository;
import com.pullit.item.dao.ItemMetadataRepository;
import com.pullit.item.embedded.ChapterHierarchy;
import com.pullit.item.embedded.CodeNamePair;
import com.pullit.item.entity.ItemHtmlData;
import com.pullit.item.entity.ItemImageData;
import com.pullit.item.entity.ItemMetadata;
import com.pullit.item.entity.Subject;
import com.pullit.itemprocess.entity.ProcessedItem;
import com.pullit.itemprocess.enums.DifficultyLevel;
import com.pullit.itemprocess.enums.ItemType;
import com.pullit.itemprocess.repository.ProcessedItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Service
public class ItemPublishService {

    private final ProcessedItemRepository processedItemRepository;
    private final OcrHistoryRepository ocrHistoryRepository;
    private final ItemMetadataRepository itemMetadataRepository;
    private final ChapterRepository chapterRepository;

    // === 매핑 규칙 (조직 표준 코드로 바꾸고 싶으면 아래 숫자만 교체) ===
    private static CodeNamePair mapQuestionForm(ItemType type) {
        return switch (type) {
            case MULTIPLE     -> new CodeNamePair(1L, "객관식");
            case SHORT_ANSWER -> new CodeNamePair(3L, "단답형");
            case SUBJECTIVE   -> new CodeNamePair(2L, "주관식");
            case ESSAY        -> new CodeNamePair(4L, "서술형");
        };
    }
    private static CodeNamePair mapDifficulty(DifficultyLevel d) {
        return switch (d) {
            case EASY   -> new CodeNamePair(1L, "쉬움");
            case MEDIUM -> new CodeNamePair(2L, "보통");
            case HARD   -> new CodeNamePair(3L, "어려움");
        };
    }

    @Transactional
    public Long publish(Long processedItemId) {
        // 1) 기본 로딩
        ProcessedItem pi = processedItemRepository.findById(processedItemId)
                .orElseThrow(() -> new IllegalArgumentException("ProcessedItem not found: " + processedItemId));

        List<OcrHistory> histories = ocrHistoryRepository.findByProcessedItemId(processedItemId);
        if (histories.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_BUSINESS_LOGIC);
        }

        // 2) Subject 역추적 (첫 이미지 기준)
        PdfImage anyImage = histories.stream()
                .map(OcrHistory::getPdfImage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No PdfImage linked in OcrHistory"));
        FileHistory fh = Optional.ofNullable(anyImage.getFileHistory())
                .orElseThrow(() -> new IllegalStateException("FileHistory not found for PdfImage " + anyImage.getId()));
        Subject subject = Optional.ofNullable(fh.getSubject())
                .orElseThrow(() -> new IllegalStateException("Subject not found from FileHistory " + fh.getId()));

        // 3) question_form / difficulty 매핑 (ENUM → CodeNamePair<Long>)
        CodeNamePair qf = mapQuestionForm(pi.getType());
        CodeNamePair diff = mapDifficulty(pi.getDifficulty());

        // 4) ChapterHierarchy 구성 (ID → name 조회)
        ChapterHierarchy ch = buildChapterHierarchy(pi);

        // 5) ItemMetadata (PK=processedItemId 재사용)
        ItemMetadata md = ItemMetadata.builder()
                .itemId(pi.getId())           // @MapsId 구조에 맞춤
                .subject(subject)
                .questionForm(qf)
                .difficulty(diff)
                .chapterHierarchy(ch)
                .passageId(pi.getPassageId())
                .build();

/*        // 6) Html/Image 데이터 합성 (OCR → html/url)
        ItemHtmlData html = buildHtmlDataFrom(histories);
        ItemImageData img = buildImageDataFrom(histories);

        // PK 공유 (@MapsId)
        html.setItemMetadata(md);
        img.setItemMetadata(md);
        md.setHtmlData(html);     // has_html_data = true (엔티티에 세터가 있다면 자동 세팅)
        md.setImageData(img);     // has_image_data = true*/

        // 7) 저장
        itemMetadataRepository.save(md);

        return md.getItemId();
    }

    private ChapterHierarchy buildChapterHierarchy(ProcessedItem pi) {
        CodeNamePair large = null, medium = null, small = null, topic = null;

        if (pi.getMajorChapterId() != null) {
            large = new CodeNamePair(pi.getMajorChapterId(), chapterName(pi.getMajorChapterId()));
        }
        if (pi.getMiddleChapterId() != null) {
            medium = new CodeNamePair(pi.getMiddleChapterId(), chapterName(pi.getMiddleChapterId()));
        }
        if (pi.getMinorChapterId() != null) {
            small = new CodeNamePair(pi.getMinorChapterId(), chapterName(pi.getMinorChapterId()));
        }
        if (pi.getTopicChapterId() != null) {
            topic = new CodeNamePair(pi.getTopicChapterId(), chapterName(pi.getTopicChapterId()));
        }
        return ChapterHierarchy.builder().largeChapter(large).mediumChapter(medium).topicChapter(topic).build();

    }

    private String chapterName(Long id) {
        if (id == null) return null;
        if (id > Integer.MAX_VALUE || id < Integer.MIN_VALUE) {
            throw new BusinessException(ErrorCode.INVALID_BUSINESS_LOGIC);
        }
        return chapterRepository.findNameById(id).describeConstable().orElse("CH-" + id);
    }

//    private ItemHtmlData buildHtmlDataFrom(List<OcrHistory> histories) {
//        // areaType 별 그룹핑
//        Map<String, List<OcrHistory>> byArea = histories.stream()
//                .filter(h -> h.getAreaType() != null)
//                .collect(Collectors.groupingBy(h -> h.getAreaType().name()));
//
//        // editedText 우선, 없으면 ocrText
//        String question = pickFirstText(byArea, "PROBLEM", "QUESTION"); // 프로젝트마다 PROBLEM/QUESTION 혼용 대비
//        String answer   = pickFirstText(byArea, "ANSWER");
//        String explain  = pickFirstText(byArea, "EXPLANATION", "EXPLAIN");
//
//        // 보기(OPTIONS) 파싱: 줄 단위 분리 (예: (1) ~, (2) ~)
//        List<String> choices = parseChoices(byArea.getOrDefault("OPTIONS", List.of()));
//
//        ItemHtmlData html = new ItemHtmlData();
//        html.setQuestionHtml(nullToEmpty(question));
//        if (choices.size() > 0) html.setChoice1Html(choices.get(0));
//        if (choices.size() > 1) html.setChoice2Html(choices.get(1));
//        if (choices.size() > 2) html.setChoice3Html(choices.get(2));
//        if (choices.size() > 3) html.setChoice4Html(choices.get(3));
//        if (choices.size() > 4) html.setChoice5Html(choices.get(4));
//        html.setAnswerHtml(nullToEmpty(answer));
//        html.setExplainText(stripHtmlIfNeeded(explain)); // 텍스트 컬럼이 따로 있으면 여기에
//        html.setExplainHtml(nullToEmpty(explain));
//        // 필요 시 passageHtml도 byArea.get("PASSAGE")에서 설정
//        return html;
//    }
//
//    private ItemImageData buildImageDataFrom(List<OcrHistory> histories) {
//        Map<String, List<OcrHistory>> byArea = histories.stream()
//                .filter(h -> h.getAreaType() != null)
//                .collect(Collectors.groupingBy(h -> h.getAreaType().name()));
//
//        String questionUrl = pickFirstUrl(byArea, "PROBLEM", "QUESTION", "IMAGE");
//        String answerUrl   = pickFirstUrl(byArea, "ANSWER");
//        String explainUrl  = pickFirstUrl(byArea, "EXPLANATION", "EXPLAIN");
//
//        ItemImageData img = new ItemImageData();
//        img.setQuestionUrl(nullToEmpty(questionUrl));
//        img.setAnswerUrl(nullToEmpty(answerUrl));
//        img.setExplainUrl(nullToEmpty(explainUrl));
//        // 필요 시 passageUrl도 설정
//        return img;
//    }

    private String pickFirstText(Map<String, List<OcrHistory>> byArea, String... keys) {
        for (String k : keys) {
            for (OcrHistory h : byArea.getOrDefault(k, List.of())) {
                String v = (h.getEditedText() != null && !h.getEditedText().isBlank())
                        ? h.getEditedText() : h.getOcrText();
                if (v != null && !v.isBlank()) return v;
            }
        }
        return null;
    }
    private String pickFirstUrl(Map<String, List<OcrHistory>> byArea, String... keys) {
        for (String k : keys) {
            for (OcrHistory h : byArea.getOrDefault(k, List.of())) {
                String v = h.getOriginalImageUrl();
                if (v != null && !v.isBlank()) return v;
            }
        }
        return null;
    }
    private List<String> parseChoices(List<OcrHistory> optionAreas) {
        // 1) editedText 우선, 2) 줄 단위 split, 3) (1),(2) 프리픽스 제거
        if (optionAreas == null || optionAreas.isEmpty()) return List.of();
        String raw = optionAreas.stream()
                .map(h -> (h.getEditedText() != null && !h.getEditedText().isBlank()) ? h.getEditedText() : h.getOcrText())
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
        if (raw.isBlank()) return List.of();
        String[] lines = raw.split("\\r?\\n");
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            String s = line.trim();
            if (s.isBlank()) continue;
            s = s.replaceFirst("^\\(?[1-5]\\)?[\\.)]\\s*", ""); // "(1) ", "1. " 등 제거
            out.add(s);
            if (out.size() == 5) break;
        }
        return out;
    }
    private String nullToEmpty(String s) { return s == null ? "" : s; }
    private String stripHtmlIfNeeded(String s) { return s == null ? "" : s.replaceAll("<[^>]+>", ""); }
}
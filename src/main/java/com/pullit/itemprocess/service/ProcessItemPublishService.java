package com.pullit.itemprocess.service;

import com.pullit.chapter.repository.ChapterRepository;
import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;
import com.pullit.filehistory.entity.FileHistory;
import com.pullit.filehistory.entity.OcrHistory;
import com.pullit.filehistory.entity.PdfImage;
import com.pullit.filehistory.repository.OcrHistoryRepository;
import com.pullit.item.embedded.ChapterHierarchy;
import com.pullit.item.embedded.CodeNamePair;
import com.pullit.item.entity.Subject;
import com.pullit.itemprocess.entity.ProcessedItem;
import com.pullit.itemprocess.entity.ProcessItemMetadata;
import com.pullit.itemprocess.entity.ProcessItemHtmlData;
import com.pullit.itemprocess.entity.ProcessItemImageData;
import com.pullit.itemprocess.enums.DifficultyLevel;
import com.pullit.itemprocess.enums.ItemType;
import com.pullit.itemprocess.repository.ProcessedItemRepository;
import com.pullit.itemprocess.repository.ProcessItemMetadataRepository;
import com.pullit.itemprocess.repository.ProcessItemHtmlDataRepository;
import com.pullit.itemprocess.repository.ProcessItemImageDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProcessItemPublishService {

    private final ProcessedItemRepository processedItemRepository;
    private final OcrHistoryRepository ocrHistoryRepository;
    private final ProcessItemMetadataRepository processItemMetadataRepository;
    private final ProcessItemHtmlDataRepository processItemHtmlDataRepository;
    private final ProcessItemImageDataRepository processItemImageDataRepository;
    private final ChapterRepository chapterRepository;

    // === 매핑 규칙 ===
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
        log.info("[Convert] start processedItemId={}", processedItemId);

        // 1) 기존 변환 데이터 정리 (재변환 허용)
        processItemMetadataRepository.findBySourceItemId(processedItemId).ifPresent(existingMeta -> {
            Long parentId = existingMeta.getItemId();
            log.info("[Convert] cleaning existing data for parentId={}, sourceItemId={}", parentId, processedItemId);
            processItemHtmlDataRepository.deleteById(parentId);
            processItemImageDataRepository.deleteById(parentId);
            processItemMetadataRepository.deleteById(parentId);
            log.info("[Convert] existing data cleaned");
        });

        // 2) 연관 데이터 로딩
        ProcessedItem pi = processedItemRepository.findForConversion(processedItemId)
                .orElseThrow(() -> new IllegalArgumentException("ProcessedItem not found: " + processedItemId));

        List<OcrHistory> histories = pi.getOcrHistories();
        if (histories.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_BUSINESS_LOGIC);
        }

        log.info("[Convert] loaded: ocrCount={}, pdfImageId={}",
                histories.size(),
                histories.get(0).getPdfImage() != null ? histories.get(0).getPdfImage().getId() : null);

        // 3) 링크/필수값 검증
        validateLinks(pi, histories);
        validateRequired(pi);

        // 4) Subject 추적
        PdfImage anyImage = histories.stream()
                .map(OcrHistory::getPdfImage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No PdfImage linked in OcrHistory"));
        FileHistory fh = Optional.ofNullable(anyImage.getFileHistory())
                .orElseThrow(() -> new IllegalStateException("FileHistory not found for PdfImage " + anyImage.getId()));
        Subject subject = Optional.ofNullable(fh.getSubject())
                .orElseThrow(() -> new IllegalStateException("Subject not found from FileHistory " + fh.getId()));

        // 5) 매핑/계층
        CodeNamePair qf = mapQuestionForm(pi.getType());
        CodeNamePair diff = mapDifficulty(pi.getDifficulty());
        ChapterHierarchy ch = buildChapterHierarchy(pi);

        // 6) 엔티티 생성 (fallback 포함 빌더 사용)
        var meta = buildMetadata(pi, subject, qf, diff, ch);
        var html = buildHtmlDataFrom(histories, pi);
        var img  = buildImageDataFrom(histories);

        // 7) 관계 묶기 (비어있으면 연결하지 않음)
        if (!isHtmlEffectivelyEmpty(html)) {
            meta.setHtmlData(html);
        } else {
            meta.setHtmlData(null);
        }
        if (img != null && img.hasImages()) {
            meta.setImageData(img);
        } else {
            meta.setImageData(null);
        }

        // 8) 부모만 저장
        ProcessItemMetadata savedMd = processItemMetadataRepository.save(meta);

        log.info("[Convert] done itemId={}", savedMd.getItemId());
        return savedMd.getItemId();
    }


    private void validateLinks(ProcessedItem pi, List<OcrHistory> histories) {
        long linkedCount = histories.stream()
                .filter(h -> h.getProcessedItem() != null && h.getProcessedItem().getId().equals(pi.getId()))
                .count();

        if (linkedCount == 0) {
            throw new IllegalStateException(
                    "No OCR histories linked to ProcessedItem " + pi.getId() +
                            ". Call confirm() first to link OCR data."
            );
        }

        log.info("[Convert] validated: {}/{} OCR histories linked", linkedCount, histories.size());
    }

    private void validateRequired(ProcessedItem pi) {
        if (pi.getType() == null) {
            throw new IllegalArgumentException("ProcessedItem.type is required");
        }
        if (pi.getDifficulty() == null) {
            throw new IllegalArgumentException("ProcessedItem.difficulty is required");
        }
        log.info("[Convert] required fields validated");
    }

    private ProcessItemMetadata buildMetadata(ProcessedItem pi, Subject subject,
                                              CodeNamePair qf, CodeNamePair diff,
                                              ChapterHierarchy ch) {
        return ProcessItemMetadata.builder()
                // itemId는 @GeneratedValue에 맡깁니다 (절대 수동 세팅 금지)
                .sourceItemId(pi.getId())     // 재변환 식별용 소스키 저장
                .subject(subject)
                .questionForm(qf)
                .difficulty(diff)
                .chapterHierarchy(ch)
                .passageId(pi.getPassageId())
                .build();
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
        return ChapterHierarchy.builder().largeChapter(large).mediumChapter(medium).smallChapter(small).topicChapter(topic).build();
    }

    private String chapterName(Long id) {
        if (id == null) return "미지정";
        try {
            String name = chapterRepository.findChapterNameByCode(id);
            return name != null ? name : "미지정";
        } catch (Exception e) {
            log.warn("[Convert] Chapter name lookup failed for id={}: {}", id, e.getMessage());
            return "미지정";
        }
    }

    private ProcessItemHtmlData buildHtmlDataFrom(List<OcrHistory> histories, ProcessedItem pi) {
        Map<String, List<OcrHistory>> byArea = histories.stream()
                .filter(h -> h.getAreaType() != null)
                .collect(Collectors.groupingBy(h -> h.getAreaType().name()));

        String passage = pickFirstText(byArea, "PASSAGE");
        String question = pickFirstText(byArea, "PROBLEM", "QUESTION");
        String answer   = pickFirstText(byArea, "ANSWER");
        String explain  = pickFirstText(byArea, "EXPLANATION", "EXPLAIN");

        if (isBlank(answer) && !isBlank(pi.getAnswer())) {
            answer = pi.getAnswer();
        }
        if (isBlank(explain)) {
            String ex1 = pi.getExplanation();
            String ex2 = pi.getSolution();
            explain = !isBlank(ex1) ? ex1 : (!isBlank(ex2) ? ex2 : null);
        }

        List<String> choices = parseChoices(byArea.getOrDefault("OPTIONS", List.of()));
        if (choices.isEmpty() && !isBlank(question)) {
            choices = tryParseChoicesFromQuestion(question);
        }

        return ProcessItemHtmlData.builder()
                .passage(toNull(passage))
                .passageHtml(toNull(passage))
                .question(toNull(question))
                .questionHtml(toNull(question))
                .choice1Html(choices.size() > 0 ? toNull(choices.get(0)) : null)
                .choice2Html(choices.size() > 1 ? toNull(choices.get(1)) : null)
                .choice3Html(choices.size() > 2 ? toNull(choices.get(2)) : null)
                .choice4Html(choices.size() > 3 ? toNull(choices.get(3)) : null)
                .choice5Html(choices.size() > 4 ? toNull(choices.get(4)) : null)
                .answer(toNull(answer))
                .answerHtml(toNull(answer))
                .explainText(toNull(stripHtmlIfNeeded(explain)))
                .explainHtml(toNull(explain))
                .build();
    }

    private ProcessItemImageData buildImageDataFrom(List<OcrHistory> histories) {
        Map<String, List<OcrHistory>> byArea = histories.stream()
                .filter(h -> h.getAreaType() != null)
                .collect(Collectors.groupingBy(h -> h.getAreaType().name()));

        String passageUrl  = pickFirstUrl(byArea, "PASSAGE");
        String questionUrl = pickFirstUrl(byArea, "PROBLEM", "QUESTION");
        String answerUrl   = pickFirstUrl(byArea, "ANSWER");
        String explainUrl  = pickFirstUrl(byArea, "EXPLANATION", "EXPLAIN");

        if (isBlank(passageUrl))  passageUrl  = pickAnyUrl(byArea);
        if (isBlank(questionUrl)) questionUrl = pickAnyUrl(byArea);
        if (isBlank(answerUrl))   answerUrl   = pickAnyUrl(byArea);
        if (isBlank(explainUrl))  explainUrl  = pickAnyUrl(byArea);

        return ProcessItemImageData.builder()
                .passageUrl(toNull(passageUrl))
                .questionUrl(toNull(questionUrl))
                .answerUrl(toNull(answerUrl))
                .explainUrl(toNull(explainUrl))
                .build();
    }

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

    // (참고) 기존 update* 메서드는 현재 플로우에선 미사용이지만 남겨둡니다.
    private void updateHtmlData(ProcessItemHtmlData existing, ProcessItemHtmlData newData) {
        existing.setPassage(newData.getPassage());
        existing.setPassageHtml(newData.getPassageHtml());
        existing.setQuestion(newData.getQuestion());
        existing.setQuestionHtml(newData.getQuestionHtml());
        existing.setChoice1Html(newData.getChoice1Html());
        existing.setChoice2Html(newData.getChoice2Html());
        existing.setChoice3Html(newData.getChoice3Html());
        existing.setChoice4Html(newData.getChoice4Html());
        existing.setChoice5Html(newData.getChoice5Html());
        existing.setAnswer(newData.getAnswer());
        existing.setAnswerHtml(newData.getAnswerHtml());
        existing.setExplainText(newData.getExplainText());
        existing.setExplainHtml(newData.getExplainHtml());
    }

    private void updateImageData(ProcessItemImageData existing, ProcessItemImageData newData) {
        existing.setPassageUrl(newData.getPassageUrl());
        existing.setQuestionUrl(newData.getQuestionUrl());
        existing.setAnswerUrl(newData.getAnswerUrl());
        existing.setExplainUrl(newData.getExplainUrl());
    }

    private boolean isHtmlEffectivelyEmpty(ProcessItemHtmlData h) {
        if (h == null) return true;
        return isBlank(h.getPassageHtml())
                && isBlank(h.getQuestionHtml())
                && isBlank(h.getChoice1Html())
                && isBlank(h.getChoice2Html())
                && isBlank(h.getChoice3Html())
                && isBlank(h.getChoice4Html())
                && isBlank(h.getChoice5Html())
                && isBlank(h.getAnswerHtml())
                && isBlank(h.getExplainHtml())
                && isBlank(h.getExplainText());
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String toNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String pickAnyUrl(Map<String, List<OcrHistory>> byArea) {
        for (List<OcrHistory> list : byArea.values()) {
            for (OcrHistory h : list) {
                String v = h.getOriginalImageUrl();
                if (!isBlank(v)) return v;
            }
        }
        return null;
    }

    private List<String> tryParseChoicesFromQuestion(String question) {
        if (isBlank(question)) return List.of();
        String[] lines = question.split("\\r?\\n");
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            String s = line.trim();
            if (s.matches("^\\(?[1-5]\\)?[\\.)].*")) {
                s = s.replaceFirst("^\\(?[1-5]\\)?[\\.)]\\s*", "");
                if (!isBlank(s)) out.add(s);
            }
        }
        return out.size() > 5 ? out.subList(0, 5) : out;
    }
}

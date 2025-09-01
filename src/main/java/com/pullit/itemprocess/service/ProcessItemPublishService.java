package com.pullit.itemprocess.service;

import com.pullit.chapter.repository.ChapterRepository;
import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;
import com.pullit.filehistory.entity.FileHistory;
import com.pullit.filehistory.entity.OcrHistory;
import com.pullit.filehistory.entity.PdfImage;
import com.pullit.item.embedded.ChapterHierarchy;
import com.pullit.item.embedded.CodeNamePair;
import com.pullit.item.entity.Subject;
import com.pullit.itemprocess.dto.request.HtmlEditorPayload;
import com.pullit.itemprocess.entity.ProcessItemHtmlData;
import com.pullit.itemprocess.entity.ProcessItemImageData;
import com.pullit.itemprocess.entity.ProcessItemMetadata;
import com.pullit.itemprocess.entity.ProcessedItem;
import com.pullit.itemprocess.enums.DifficultyLevel;
import com.pullit.itemprocess.enums.ItemType;
import com.pullit.itemprocess.repository.ProcessItemHtmlDataRepository;
import com.pullit.itemprocess.repository.ProcessItemImageDataRepository;
import com.pullit.itemprocess.repository.ProcessItemMetadataRepository;
import com.pullit.itemprocess.repository.ProcessedItemRepository;
import io.micrometer.common.lang.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProcessItemPublishService {

    private final ProcessedItemRepository processedItemRepository;
    private final ProcessItemMetadataRepository processItemMetadataRepository;
    private final ProcessItemHtmlDataRepository processItemHtmlDataRepository;
    private final ProcessItemImageDataRepository processItemImageDataRepository;
    private final ChapterRepository chapterRepository;

    // ===== 매핑 규칙 =====
    private static CodeNamePair mapQuestionForm(ItemType type) {
        return switch (type) {
            case FIVE_CHOICE            -> new CodeNamePair(50L, "5지 선택");
            case SHORT_ANSWER_ORDERED   -> new CodeNamePair(60L, "단답 유순형");
            case SHORT_ANSWER_UNORDERED -> new CodeNamePair(61L, "단답 무순형");
            case FREE_CHOICE            -> new CodeNamePair(10L, "자유 선지형");
        };
    }

    private static CodeNamePair mapDifficulty(DifficultyLevel d) {
        return switch (d) {
            case EASY   -> new CodeNamePair(1L, "하");
            case MEDIUM -> new CodeNamePair(3L, "중");
            case HARD   -> new CodeNamePair(5L, "상");
        };
    }

    // ===== 공개 API =====
    @Transactional
    public Long publish(Long processedItemId) {
        return publish(processedItemId, null);
    }

    @Transactional
    public Long publish(Long processedItemId, @Nullable HtmlEditorPayload editorPayload) {
        log.info("[Publish] start processedItemId={}, withEditor={}", processedItemId, editorPayload != null);

        // 1) 대상 로딩(+OCR fetch join) 및 선검증
        ProcessedItem pi = processedItemRepository.findForConversion(processedItemId)
                .orElseThrow(() -> new IllegalArgumentException("ProcessedItem not found: " + processedItemId));

        List<OcrHistory> histories = pi.getOcrHistories();
        if (histories == null || histories.isEmpty()) throw new BusinessException(ErrorCode.INVALID_BUSINESS_LOGIC);

        validateLinks(pi, histories);
        validateRequired(pi);

        // 2) Subject 역추적 (임의 이미지→FileHistory→Subject)
        PdfImage anyImage = histories.stream()
                .map(OcrHistory::getPdfImage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No PdfImage linked"));
        FileHistory fh = Optional.ofNullable(anyImage.getFileHistory())
                .orElseThrow(() -> new IllegalStateException("FileHistory not found"));
        Subject subject = Optional.ofNullable(fh.getSubject())
                .orElseThrow(() -> new IllegalStateException("Subject not found"));

        // 3) 매핑/챕터
        CodeNamePair qf   = mapQuestionForm(pi.getType());
        CodeNamePair diff = mapDifficulty(pi.getDifficulty());
        ChapterHierarchy ch = buildChapterHierarchy(pi);

        // 4) 메타/HTML/이미지 빌드
        ProcessItemMetadata meta = buildMetadata(pi, subject, qf, diff, ch);
        ProcessItemHtmlData html = buildHtmlDataFrom(histories, pi);   // OCR 기반 초안
        ProcessItemImageData img = buildImageDataFrom(histories);      // OCR 이미지 URL

        // 5) 에디터 입력으로 override(+ sanitize + TEXT 보강)
        if (editorPayload != null) {
            applyEditor(html, editorPayload);
        }

        // 6) 저장 순서: 부모(@Id) → 자식(@MapsId)
        ProcessItemMetadata savedMd = processItemMetadataRepository.save(meta);

        html.setItemMetadata(savedMd); // @MapsId 매핑
        processItemHtmlDataRepository.save(html);

        img.setItemMetadata(savedMd);  // @MapsId 매핑
        processItemImageDataRepository.save(img);

        // 7) 메타 플래그 세팅(양방향 편의)
        savedMd.setHtmlData(html);
        savedMd.setImageData(img);
        savedMd.setHasHtmlData(!isHtmlEffectivelyEmpty(html));
        savedMd.setHasImageData(img.hasImages());

        log.info("[Publish] done itemId={}", savedMd.getItemId());
        return savedMd.getItemId();
    }

    // ===== 에디터 적용 & Sanitize =====
    private void applyEditor(ProcessItemHtmlData html, HtmlEditorPayload p) {
        // 허용 태그/속성/프로토콜 (img src가 지워지지 않도록 data/http/https 허용)
        Safelist safe = Safelist.relaxed()
                .addTags("span","div","p","br","ul","ol","li","sup","sub",
                        "table","thead","tbody","tr","td","th","img")
                .addAttributes("span","class","style")
                .addAttributes("div","class","style")
                .addAttributes("p","class","style")
                .addAttributes("table","class","style","border","cellpadding","cellspacing")
                .addAttributes("td","class","style","colspan","rowspan","align","valign","width","height")
                .addAttributes("th","class","style","colspan","rowspan","align","valign","width","height")
                .addAttributes("img","src","alt","width","height","style")
                .addProtocols("img", "src", "http", "https", "data");

        // HTML 필드 override (incoming이 비어있으면 기존 유지)
        html.setPassageHtml(  pickSanitizedOr(html.getPassageHtml(),  p.getPassageHtml(),  safe) );
        html.setQuestionHtml( pickSanitizedOr(html.getQuestionHtml(), p.getQuestionHtml(), safe) );
        html.setChoice1Html(  pickSanitizedOr(html.getChoice1Html(),  p.getChoice1Html(),  safe) );
        html.setChoice2Html(  pickSanitizedOr(html.getChoice2Html(),  p.getChoice2Html(),  safe) );
        html.setChoice3Html(  pickSanitizedOr(html.getChoice3Html(),  p.getChoice3Html(),  safe) );
        html.setChoice4Html(  pickSanitizedOr(html.getChoice4Html(),  p.getChoice4Html(),  safe) );
        html.setChoice5Html(  pickSanitizedOr(html.getChoice5Html(),  p.getChoice5Html(),  safe) );
        html.setAnswerHtml(   pickSanitizedOr(html.getAnswerHtml(),   p.getAnswerHtml(),   safe) );
        html.setExplainHtml(  pickSanitizedOr(html.getExplainHtml(),  p.getExplainHtml(),  safe) );

        // TEXT 컬럼(우선순위: FE 전달 text → HTML에서 추출 → 기존값 유지)
        html.setPassage(     nonBlankOr(html.getPassage(),     firstNonBlank(p.getPassageText(),  extractText(html.getPassageHtml()))) );
        html.setQuestion(    nonBlankOr(html.getQuestion(),    firstNonBlank(p.getQuestionText(), extractText(html.getQuestionHtml()))) );
        html.setAnswer(      nonBlankOr(html.getAnswer(),      firstNonBlank(p.getAnswerText(),   extractText(html.getAnswerHtml()))) );
        html.setExplainText( nonBlankOr(html.getExplainText(), firstNonBlank(p.getExplainText(),  extractText(html.getExplainHtml()))) );
    }

    // ===== 메타/챕터/HTML/이미지 빌더 =====
    private ProcessItemMetadata buildMetadata(ProcessedItem pi, Subject subject,
                                              CodeNamePair qf, CodeNamePair diff,
                                              ChapterHierarchy ch) {
        return ProcessItemMetadata.builder()
                .sourceItemId(pi.getId())     // 재변환 추적용
                .subject(subject)
                .questionForm(qf)
                .difficulty(diff)
                .chapterHierarchy(ch)
                .passageId(pi.getPassageId())
                .build();
    }

    private ChapterHierarchy buildChapterHierarchy(ProcessedItem pi) {
        CodeNamePair large = null, medium = null, small = null, topic = null;

        if (pi.getMajorChapterId()  != null) large = new CodeNamePair(pi.getMajorChapterId(),  chapterName(pi.getMajorChapterId()));
        if (pi.getMiddleChapterId() != null) medium = new CodeNamePair(pi.getMiddleChapterId(), chapterName(pi.getMiddleChapterId()));
        if (pi.getMinorChapterId()  != null) small = new CodeNamePair(pi.getMinorChapterId(),  chapterName(pi.getMinorChapterId()));
        if (pi.getTopicChapterId()  != null) topic = new CodeNamePair(pi.getTopicChapterId(),  chapterName(pi.getTopicChapterId()));

        return ChapterHierarchy.builder()
                .largeChapter(large)
                .mediumChapter(medium)
                .smallChapter(small)
                .topicChapter(topic)
                .build();
    }

    private String chapterName(Long id) {
        if (id == null) return "미지정";
        try {
            String name = chapterRepository.findChapterNameByCode(id);
            return name != null ? name : "미지정";
        } catch (Exception e) {
            log.warn("[Publish] Chapter name lookup failed for id={}: {}", id, e.getMessage());
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

        // 백필(fallback) – ProcessedItem 직렬 텍스트
        if (isBlank(answer) && !isBlank(pi.getAnswer())) answer = pi.getAnswer();
        if (isBlank(explain)) {
            explain = !isBlank(pi.getExplanation()) ? pi.getExplanation()
                    : (!isBlank(pi.getSolution()) ? pi.getSolution() : null);
        }

        // 보기 파싱
        List<String> choices = parseChoices(byArea.getOrDefault("OPTIONS", List.of()));
        if (choices.isEmpty() && !isBlank(question)) {
            choices = tryParseChoicesFromQuestion(question);
        }

        // OCR 텍스트를 최소 HTML로 감싸서 저장(편집기/뷰어 안정성)
        return ProcessItemHtmlData.builder()
                .passage(toNull(passage))
                .passageHtml(textToBasicHtml(passage))
                .question(toNull(question))
                .questionHtml(textToBasicHtml(question))
                .choice1Html(choices.size() > 0 ? textToBasicHtml(choices.get(0)) : null)
                .choice2Html(choices.size() > 1 ? textToBasicHtml(choices.get(1)) : null)
                .choice3Html(choices.size() > 2 ? textToBasicHtml(choices.get(2)) : null)
                .choice4Html(choices.size() > 3 ? textToBasicHtml(choices.get(3)) : null)
                .choice5Html(choices.size() > 4 ? textToBasicHtml(choices.get(4)) : null)
                .answer(toNull(answer))
                .answerHtml(textToBasicHtml(answer))
                .explainText(toNull(stripHtmlIfNeeded(explain)))
                .explainHtml(textToBasicHtml(explain))
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

        // 비어 있으면 임의 URL 백필(뷰어 안정성)
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

    // ===== 검증 & 유틸 =====
    private void validateLinks(ProcessedItem pi, List<OcrHistory> histories) {
        long linkedCount = histories.stream()
                .filter(h -> h.getProcessedItem() != null && Objects.equals(h.getProcessedItem().getId(), pi.getId()))
                .count();
        if (linkedCount == 0) {
            throw new IllegalStateException("No OCR histories linked to ProcessedItem " + pi.getId() +
                    ". Call confirm() first to link OCR data.");
        }
        log.info("[Publish] validated: {}/{} OCR histories linked", linkedCount, histories.size());
    }

    private void validateRequired(ProcessedItem pi) {
        if (pi.getType() == null)        throw new IllegalArgumentException("ProcessedItem.type is required");
        if (pi.getDifficulty() == null)  throw new IllegalArgumentException("ProcessedItem.difficulty is required");
        log.info("[Publish] required fields validated");
    }

    private String pickSanitizedOr(String current, String incoming, Safelist safe) {
        if (isBlank(incoming)) return current; // 현재 값 유지
        return Jsoup.clean(incoming, safe);
    }

    private String extractText(String html) {
        if (isBlank(html)) return "";
        return Jsoup.parse(html).text();
    }

    private String firstNonBlank(String a, String b) {
        return !isBlank(a) ? a : (!isBlank(b) ? b : "");
    }

    private String nonBlankOr(String current, String fallback) {
        return !isBlank(current) ? current : nullToEmpty(fallback);
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

    private boolean notBlank(String s) { return s != null && !s.trim().isEmpty(); }
    private boolean isBlank(String s)  { return s == null || s.trim().isEmpty(); }
    private String  nullToEmpty(String s) { return s == null ? "" : s; }

    private String stripHtmlIfNeeded(String s) {
        return s == null ? "" : s.replaceAll("<[^>]+>", "");
    }

    private String toNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String textToBasicHtml(String text) {
        if (isBlank(text)) return null;
        // TEXT를 안전하게 escape → 줄바꿈을 <br>로 → <p> 래핑
        String cleanText = Jsoup.clean(text, Safelist.none());
        return "<p>" + cleanText.replace("\r\n", "\n").replace("\n", "<br>") + "</p>";
    }

    private String pickFirstText(Map<String, List<OcrHistory>> byArea, String... keys) {
        for (String k : keys) {
            for (OcrHistory h : byArea.getOrDefault(k, List.of())) {
                String v = notBlank(h.getEditedText()) ? h.getEditedText() : h.getOcrText();
                if (notBlank(v)) return v;
            }
        }
        return null;
    }

    private String pickFirstUrl(Map<String, List<OcrHistory>> byArea, String... keys) {
        for (String k : keys) {
            for (OcrHistory h : byArea.getOrDefault(k, List.of())) {
                String v = h.getOriginalImageUrl();
                if (notBlank(v)) return v;
            }
        }
        return null;
    }

    private String pickAnyUrl(Map<String, List<OcrHistory>> byArea) {
        for (List<OcrHistory> list : byArea.values()) {
            for (OcrHistory h : list) {
                String v = h.getOriginalImageUrl();
                if (notBlank(v)) return v;
            }
        }
        return null;
    }

    private List<String> parseChoices(List<OcrHistory> optionAreas) {
        if (optionAreas == null || optionAreas.isEmpty()) return List.of();
        String raw = optionAreas.stream()
                .map(h -> notBlank(h.getEditedText()) ? h.getEditedText() : h.getOcrText())
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
        if (isBlank(raw)) return List.of();

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

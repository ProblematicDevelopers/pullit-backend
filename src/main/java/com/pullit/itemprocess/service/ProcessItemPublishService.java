package com.pullit.itemprocess.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pullit.chapter.repository.ChapterRepository;
import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;
import com.pullit.common.s3.dto.S3UploadRequest;
import com.pullit.common.s3.dto.S3UploadResponse;
import com.pullit.common.s3.enums.S3Directory;
import com.pullit.common.s3.service.S3Service;
import com.pullit.filehistory.entity.FileHistory;
import com.pullit.filehistory.entity.OcrHistory;
import com.pullit.filehistory.entity.PdfImage;
import com.pullit.filehistory.repository.OcrHistoryRepository;
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
import com.pullit.itemprocess.util.SvgGenerator;

import io.micrometer.common.lang.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    private final S3Service s3Service;
    private final SvgGenerator svgGenerator;

    // === 매핑 규칙 ===
    private static CodeNamePair mapQuestionForm(ItemType type) {
        return switch (type) {
            case FIVE_CHOICE     -> new CodeNamePair(50L, "5지 선택");
            case SHORT_ANSWER_ORDERED -> new CodeNamePair(60L, "단답 유순형");
            case SHORT_ANSWER_UNORDERED   -> new CodeNamePair(61L, "단답 무순형");
            case FREE_CHOICE        -> new CodeNamePair(10L, "자유 선지형");
        };
    }

    private static CodeNamePair mapDifficulty(DifficultyLevel d) {
        return switch (d) {
            case EASY   -> new CodeNamePair(1L, "하");
            case MEDIUM -> new CodeNamePair(3L, "중");
            case HARD   -> new CodeNamePair(5L, "상");
        };
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

    private boolean hasAnyHtml(ProcessItemHtmlData h) {
        return notBlank(h.getPassageHtml()) || notBlank(h.getQuestionHtml()) ||
                notBlank(h.getChoice1Html()) || notBlank(h.getChoice2Html()) ||
                notBlank(h.getChoice3Html()) || notBlank(h.getChoice4Html()) ||
                notBlank(h.getChoice5Html()) || notBlank(h.getAnswerHtml()) ||
                notBlank(h.getExplainHtml());
    }

    private boolean notBlank(String s) { return s != null && !s.trim().isEmpty(); }


    /** 기존 값 keep, 새 값이 비어있지 않으면 sanitize 후 교체 */
    private String pickSanitizedOr(String current, String incoming, Safelist safe) {
        if (isBlank(incoming)) return nullToEmpty(current);
        String cleaned = Jsoup.clean(incoming, safe);
        return cleaned;
    }

    @Transactional
    public Long publish(Long processedItemId, @Nullable HtmlEditorPayload editorPayload) {
        log.info("[Publish] start processedItemId={}, withEditor={}", processedItemId, editorPayload != null);

        // 1) 대상 로딩 및 검증
        ProcessedItem pi = processedItemRepository.findForConversion(processedItemId)
                .orElseThrow(() -> new IllegalArgumentException("ProcessedItem not found: " + processedItemId));
        List<OcrHistory> histories = pi.getOcrHistories();
        if (histories.isEmpty()) throw new BusinessException(ErrorCode.INVALID_BUSINESS_LOGIC);

        validateLinks(pi, histories);
        validateRequired(pi);

        PdfImage anyImage = histories.stream()
                .map(OcrHistory::getPdfImage).filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new IllegalStateException("No PdfImage linked"));
        FileHistory fh = Optional.ofNullable(anyImage.getFileHistory())
                .orElseThrow(() -> new IllegalStateException("FileHistory not found"));
        Subject subject = Optional.ofNullable(fh.getSubject())
                .orElseThrow(() -> new IllegalStateException("Subject not found"));

        CodeNamePair qf   = mapQuestionForm(pi.getType());
        CodeNamePair diff = mapDifficulty(pi.getDifficulty());
        ChapterHierarchy ch = buildChapterHierarchy(pi);

        // 2) 부모 메타 신규 생성 (항상 새 row)
        ProcessItemMetadata meta = buildMetadata(pi, subject, qf, diff, ch);

        // 3) 자식(HTML/IMAGE) 초안 생성 - 새 엔티티이어야 함(아이디 null 상태!)
        ProcessItemHtmlData html = buildHtmlDataFrom(histories, pi);
        ProcessItemImageData img = buildImageDataFrom(histories);

        // 3-1) FE 에디터 값이 오면 TinyMCE 보안 강화 적용
        if (editorPayload != null) {
            applyTinyMceEditor(html, editorPayload);
        }

        // 4) 부모-자식 결합 (연관 설정) - @OneToOne(or @OneToMany) + cascade=ALL 전제
        //    PK를 강제로 채우지 말고, @MapsId 사용 시에는 '부모 set'만 하고 PK는 JPA에 맡깁니다.
        html.setItemMetadata(meta);
        img.setItemMetadata(meta);

        // 혹시 모르게 값이 들어있다면 PK/버전 클리어(낙관잠금/merge 방지)
        tryClearIdentity(html);
        tryClearIdentity(img);

        meta.setHtmlData(html);
        meta.setImageData(img);
        meta.setHasHtmlData(hasAnyHtml(html));
        meta.setHasImageData(img.hasImages());
        
        log.info("[Publish] 연관관계 설정 완료 - htmlData: {}, imageData: {}", 
                html != null ? "있음" : "없음", img != null ? "있음" : "없음");

        // 5) 단일 save로 cascade persist
        ProcessItemMetadata saved = processItemMetadataRepository.saveAndFlush(meta);
        
        log.info("[Publish] ProcessItemMetadata 저장 완료 - itemId: {}, htmlData: {}, imageData: {}", 
                saved.getItemId(), 
                saved.getHtmlData() != null ? "저장됨" : "없음",
                saved.getImageData() != null ? "저장됨" : "없음");

        // 6) SVG 생성 및 S3 업로드 (별도 트랜잭션으로 처리)
        try {
            generateAndUploadSvgsAsync(saved, html);
            log.info("[Publish] SVG 생성 및 업로드 시작 - itemId={}", saved.getItemId());
        } catch (Exception e) {
            log.error("[Publish] SVG 생성 및 업로드 실패 - itemId={}", saved.getItemId(), e);
            // SVG 업로드 실패는 전체 프로세스를 중단시키지 않음
        }

        log.info("[Publish] done itemId={}", saved.getItemId());
        return saved.getItemId();
    }

    /** child 쪽에 ID나 @Version 값이 실수로 세팅돼 들어오는 경우 merge 대신 persist가 되도록 비움 */
    private void tryClearIdentity(Object child) {
        try {
            var idField = child.getClass().getDeclaredField("itemId"); // @MapsId 필드명에 맞춰 수정
            idField.setAccessible(true);
            idField.set(child, null);
        } catch (NoSuchFieldException ignored) {
        } catch (IllegalAccessException e) {
            log.warn("[Publish] clear id failed: {}", e.getMessage());
        }
        try {
            var ver = child.getClass().getDeclaredField("version"); // @Version 쓰는 경우만
            ver.setAccessible(true);
            ver.set(child, null);
        } catch (NoSuchFieldException ignored) {
        } catch (IllegalAccessException e) {
            log.warn("[Publish] clear version failed: {}", e.getMessage());
        }
    }
    private String textToBasicHtml(String text) {
        if (isBlank(text)) return null;
        return "<p>" + Jsoup.clean(text, Safelist.none())
                .replace("\n", "<br>") + "</p>";
    }
    /** 에디터 HTML로 필드별 override + sanitize + TEXT 자동 세팅 */
    private void applyEditor(ProcessItemHtmlData html, HtmlEditorPayload p) {
        // 1) sanitize set (허용 태그 확장: span/div/p/br/ul/ol/li/b/i/u/sup/sub/table/thead/tbody/tr/td/th/img)
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
                // ✅ 이 부분이 없으면 img src가 날아감
                .addProtocols("img", "src", "http", "https", "data");

        // 2) 필드별 override 도우미
        html.setPassageHtml( pickSanitizedOr(html.getPassageHtml(),  p.getPassageHtml(),  safe) );
        html.setQuestionHtml( pickSanitizedOr(html.getQuestionHtml(), p.getQuestionHtml(), safe) );
        html.setChoice1Html( pickSanitizedOr(html.getChoice1Html(),  p.getChoice1Html(),  safe) );
        html.setChoice2Html( pickSanitizedOr(html.getChoice2Html(),  p.getChoice2Html(),  safe) );
        html.setChoice3Html( pickSanitizedOr(html.getChoice3Html(),  p.getChoice3Html(),  safe) );
        html.setChoice4Html( pickSanitizedOr(html.getChoice4Html(),  p.getChoice4Html(),  safe) );
        html.setChoice5Html( pickSanitizedOr(html.getChoice5Html(),  p.getChoice5Html(),  safe) );
        html.setAnswerHtml(  pickSanitizedOr(html.getAnswerHtml(),   p.getAnswerHtml(),   safe) );
        html.setExplainHtml( pickSanitizedOr(html.getExplainHtml(),  p.getExplainHtml(),  safe) );

        // 3) TEXT 컬럼 채우기 (FE가 넘겨주면 우선, 없으면 서버가 HTML→Text 추출)
        html.setPassage( nonBlankOr(html.getPassage(),  firstNonBlank(p.getPassageText(),  extractText(html.getPassageHtml()))) );
        html.setQuestion( nonBlankOr(html.getQuestion(), firstNonBlank(p.getQuestionText(), extractText(html.getQuestionHtml()))) );
        html.setAnswer( nonBlankOr(html.getAnswer(),     firstNonBlank(p.getAnswerText(),   extractText(html.getAnswerHtml()))) );
        html.setExplainText( nonBlankOr(html.getExplainText(), firstNonBlank(p.getExplainText(), extractText(html.getExplainHtml()))) );
    }

    /** TinyMCE 에디터 전용 HTML 처리 (보안 강화) */
    private void applyTinyMceEditor(ProcessItemHtmlData html, HtmlEditorPayload p) {
        log.info("[TinyMCE] Applying editor HTML with enhanced security");
        
        // TinyMCE용 강화된 보안 Safelist
        Safelist tinyMceSafe = createTinyMceSafelist();
        
        // 필드별 override (강화된 보안 적용)
        html.setPassageHtml( pickSanitizedOr(html.getPassageHtml(),  p.getPassageHtml(),  tinyMceSafe) );
        html.setQuestionHtml( pickSanitizedOr(html.getQuestionHtml(), p.getQuestionHtml(), tinyMceSafe) );
        html.setChoice1Html( pickSanitizedOr(html.getChoice1Html(),  p.getChoice1Html(),  tinyMceSafe) );
        html.setChoice2Html( pickSanitizedOr(html.getChoice2Html(),  p.getChoice2Html(),  tinyMceSafe) );
        html.setChoice3Html( pickSanitizedOr(html.getChoice3Html(),  p.getChoice3Html(),  tinyMceSafe) );
        html.setChoice4Html( pickSanitizedOr(html.getChoice4Html(),  p.getChoice4Html(),  tinyMceSafe) );
        html.setChoice5Html( pickSanitizedOr(html.getChoice5Html(),  p.getChoice5Html(),  tinyMceSafe) );
        html.setAnswerHtml(  pickSanitizedOr(html.getAnswerHtml(),   p.getAnswerHtml(),   tinyMceSafe) );
        html.setExplainHtml( pickSanitizedOr(html.getExplainHtml(),  p.getExplainHtml(),  tinyMceSafe) );

        // TEXT 컬럼 채우기 (기존과 동일)
        html.setPassage( nonBlankOr(html.getPassage(),  firstNonBlank(p.getPassageText(),  extractText(html.getPassageHtml()))) );
        html.setQuestion( nonBlankOr(html.getQuestion(), firstNonBlank(p.getQuestionText(), extractText(html.getQuestionHtml()))) );
        html.setAnswer( nonBlankOr(html.getAnswer(),     firstNonBlank(p.getAnswerText(),   extractText(html.getAnswerHtml()))) );
        html.setExplainText( nonBlankOr(html.getExplainText(), firstNonBlank(p.getExplainText(), extractText(html.getExplainHtml()))) );
    }

    /** TinyMCE용 강화된 보안 Safelist 생성 */
    private Safelist createTinyMceSafelist() {
        return Safelist.relaxed()
                .addTags("span","div","p","br","ul","ol","li","sup","sub",
                        "table","thead","tbody","tr","td","th","img","strong","em","u")
                // 안전한 속성만 허용 (style 제거로 인라인 스타일 차단)
                .addAttributes("span","class","data-latex")
                .addAttributes("div","class")
                .addAttributes("p","class")
                .addAttributes("table","class","border","cellpadding","cellspacing")
                .addAttributes("td","class","colspan","rowspan","align","valign")
                .addAttributes("th","class","colspan","rowspan","align","valign")
                .addAttributes("img","src","alt","width","height")
                // HTTPS와 프록시 URL만 허용 (data: 차단)
                .addProtocols("img", "src", "https")
                .addProtocols("img", "src", "/api/image/proxy") // 프록시 URL 허용
                // 모든 이벤트 핸들러 제거
                .removeAttributes("*", "onclick", "onload", "onerror", "onmouseover", "onmouseout", 
                                "onfocus", "onblur", "onchange", "onsubmit", "onreset", "style");
                // javascript: 프로토콜은 addProtocols로 허용된 것만 사용하므로 별도 제거 불필요
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

    /**
     * SVG 생성 및 S3 업로드 (비동기 처리)
     */
    private void generateAndUploadSvgsAsync(ProcessItemMetadata metadata, ProcessItemHtmlData htmlData) {
        // 별도 스레드에서 처리하여 메인 트랜잭션에 영향 주지 않음
        new Thread(() -> {
            try {
                generateAndUploadSvgs(metadata, htmlData);
            } catch (Exception e) {
                log.error("[Publish] 비동기 SVG 업로드 실패 - itemId={}", metadata.getItemId(), e);
            }
        }).start();
    }

    /**
     * SVG 생성 및 S3 업로드
     */
    private void generateAndUploadSvgs(ProcessItemMetadata metadata, ProcessItemHtmlData htmlData) {
        Long itemId = metadata.getItemId();
        log.info("[Publish] SVG 생성 및 업로드 시작 - itemId={}", itemId);

        try {
            // 1. 지문 SVG 생성 및 업로드
            log.info("[Publish] 지문 SVG 생성 시작 - itemId={}", itemId);
            String passageSvg = svgGenerator.generatePassageSvg(htmlData, itemId);
            String passageUrl = uploadSvgToS3(passageSvg, itemId, "passage", S3Directory.SVG_PASSAGE);
            log.info("[Publish] 지문 SVG 업로드 완료 - itemId={}, url={}", itemId, passageUrl);

            // 2. 문제 SVG 생성 및 업로드
            log.info("[Publish] 문제 SVG 생성 시작 - itemId={}", itemId);
            String questionSvg = svgGenerator.generateQuestionSvg(htmlData, itemId);
            String questionUrl = uploadSvgToS3(questionSvg, itemId, "question", S3Directory.SVG_QUESTION);
            log.info("[Publish] 문제 SVG 업로드 완료 - itemId={}, url={}", itemId, questionUrl);

            // 3. 답안 SVG 생성 및 업로드
            log.info("[Publish] 답안 SVG 생성 시작 - itemId={}", itemId);
            String answerSvg = svgGenerator.generateAnswerSvg(htmlData, itemId);
            String answerUrl = uploadSvgToS3(answerSvg, itemId, "answer", S3Directory.SVG_ANSWER);
            log.info("[Publish] 답안 SVG 업로드 완료 - itemId={}, url={}", itemId, answerUrl);

            // 4. 해설 SVG 생성 및 업로드
            log.info("[Publish] 해설 SVG 생성 시작 - itemId={}", itemId);
            String explainSvg = svgGenerator.generateExplainSvg(htmlData, itemId);
            String explainUrl = uploadSvgToS3(explainSvg, itemId, "explain", S3Directory.SVG_EXPLAIN);
            log.info("[Publish] 해설 SVG 업로드 완료 - itemId={}, url={}", itemId, explainUrl);

            // 5. ProcessItemImageData 업데이트
            log.info("[Publish] ProcessItemImageData 업데이트 시작 - itemId={}", itemId);
            updateImageDataWithSvgUrls(metadata, passageUrl, questionUrl, answerUrl, explainUrl);

            log.info("[Publish] SVG 업로드 전체 완료 - itemId={}, passageUrl={}, questionUrl={}, answerUrl={}, explainUrl={}", 
                    itemId, passageUrl, questionUrl, answerUrl, explainUrl);

        } catch (Exception e) {
            log.error("[Publish] SVG 생성 및 업로드 중 오류 발생 - itemId={}", itemId, e);
            throw new RuntimeException("SVG 생성 및 업로드 실패", e);
        }
    }

    /**
     * SVG를 S3에 업로드
     */
    private String uploadSvgToS3(String svgContent, Long itemId, String type, S3Directory directory) {
        try {
            // 파일명 생성: {itemId}_{type}_{timestamp}.svg
            String timestamp = java.time.LocalDate.now().toString();
            String fileName = String.format("%d_%s_%s.svg", itemId, type, timestamp);

            // SVG 내용을 바이트 배열로 변환
            byte[] svgBytes = svgContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            // S3 업로드 요청 생성
            S3UploadRequest uploadRequest = S3UploadRequest.builder()
                    .fileData(svgBytes)
                    .fileName(fileName)
                    .directory(directory)
                    .contentType("image/svg+xml")
                    .publicRead(true) // public 접근 허용
                    .build();

            // S3 업로드 실행
            S3UploadResponse response = s3Service.upload(uploadRequest);

            // Public URL 생성 (S3 버킷의 public URL)
            String publicUrl = generatePublicUrl(response.getS3Key());

            log.info("[Publish] SVG 업로드 성공 - fileName={}, s3Key={}, publicUrl={}", 
                    fileName, response.getS3Key(), publicUrl);

            return publicUrl;

        } catch (Exception e) {
            log.error("[Publish] SVG 업로드 실패 - itemId={}, type={}", itemId, type, e);
            throw new RuntimeException("SVG 업로드 실패: " + type, e);
        }
    }

    /**
     * Public URL 생성
     */
    private String generatePublicUrl(String s3Key) {
        // S3 버킷의 public URL 형식: https://{bucket-name}.s3.{region}.amazonaws.com/{key}
        // 또는 CloudFront 도메인을 사용할 수 있음
        return String.format("https://img.chunjae-platform.com/upload/capture/tsherpa/%s", s3Key);
    }

    /**
     * ProcessItemImageData에 SVG URL 업데이트
     */
    @Transactional
    private void updateImageDataWithSvgUrls(ProcessItemMetadata metadata, String passageUrl, 
                                          String questionUrl, String answerUrl, String explainUrl) {
        try {
            // DB에서 최신 ProcessItemImageData 조회
            ProcessItemImageData imageData = processItemImageDataRepository.findById(metadata.getItemId())
                .orElse(null);
            
            if (imageData == null) {
                log.warn("[Publish] ProcessItemImageData를 찾을 수 없습니다 - itemId={}", metadata.getItemId());
                return;
            }

            // SVG URL 설정
            imageData.setPassageUrl(passageUrl);
            imageData.setQuestionUrl(questionUrl);
            imageData.setAnswerUrl(answerUrl);
            imageData.setExplainUrl(explainUrl);

            // DB 업데이트
            ProcessItemImageData savedImageData = processItemImageDataRepository.save(imageData);

            log.info("[Publish] ProcessItemImageData 업데이트 완료 - itemId={}, passageUrl={}, questionUrl={}, answerUrl={}, explainUrl={}", 
                    metadata.getItemId(), passageUrl, questionUrl, answerUrl, explainUrl);

        } catch (Exception e) {
            log.error("[Publish] ProcessItemImageData 업데이트 실패 - itemId={}", metadata.getItemId(), e);
            throw new RuntimeException("ProcessItemImageData 업데이트 실패", e);
        }
    }
}

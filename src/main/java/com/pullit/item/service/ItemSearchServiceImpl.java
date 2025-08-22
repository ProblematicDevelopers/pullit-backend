package com.pullit.item.service;

import com.pullit.common.annotation.LoggingTrace;
import com.pullit.item.dao.ItemHtmlDataRepository;
import com.pullit.item.dao.ItemImageDataRepository;
import com.pullit.item.dao.ItemMetadataRepository;
import com.pullit.item.dto.request.ItemSearchRequest;
import com.pullit.item.dto.response.ItemSearchResponse;
import com.pullit.item.entity.ItemHtmlData;
import com.pullit.item.entity.ItemImageData;
import com.pullit.item.entity.ItemMetadata;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemSearchServiceImpl implements ItemSearchService {

    private final ItemHtmlDataRepository itemHtmlDataRepository;
    private final ItemImageDataRepository itemImageDataRepository;
    private final ItemMetadataRepository itemMetadataRepository;

    @Override
    @LoggingTrace(level = LoggingTrace.LogLevel.INFO, logExecutionTime = true, logParameters = true)
    public Page<ItemSearchResponse> searchItems(ItemSearchRequest request) {
        log.info("[인덱스 없음] 문항 검색 시작: {}",request);

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        Page<ItemMetadata> itemPage = itemMetadataRepository.searchItems(request, pageable);
        log.info("검색 결과: {} 건", itemPage.getTotalElements());

        return itemPage.map(this::convertToResponse);
    }

    @Override
    public ItemSearchResponse getItemDetail(Long itemId) {
        ItemMetadata metadata = itemMetadataRepository.findByItemId(itemId)
                .orElseThrow(() -> new EntityNotFoundException("문항을 찾을 수 없습니다: " + itemId));

        return convertToResponse(metadata);
    }

    @Override
    // @Cacheable(value = "chapterItemCounts", key = "#subjectId + '-' + #chapterIds.hashCode()") // 캐시 비활성화 - 성능 측정
    @LoggingTrace(level = LoggingTrace.LogLevel.INFO, logExecutionTime = true)
    public Map<Long, Long> getItemCountsByChapters(Long subjectId, List<Long> chapterIds) {
        log.info("[인덱스 없음] 챕터별 문항 수 집계: subjectId={}, chapterIds={}", subjectId, chapterIds);
        return itemMetadataRepository.countItemsByChapters(subjectId, chapterIds);
    }

    @Override
    // @Cacheable(value = "difficultyItemCounts", key = "#subjectId") // 캐시 비활성화 - 성능 측정
    @LoggingTrace(level = LoggingTrace.LogLevel.INFO, logExecutionTime = true)
    public Map<Long, Long> getItemCountsByDifficulty(Long subjectId) {
        log.info("[인덱스 없음] 난이도별 문항 수 집계: subjectId={}", subjectId);
        return itemMetadataRepository.countItemsByDifficulty(subjectId);
    }

    @Override
    // @Cacheable(value = "questionFormItemCounts", key = "#subjectId") // 캐시 비활성화 - 성능 측정
    @LoggingTrace(level = LoggingTrace.LogLevel.INFO, logExecutionTime = true)
    public Map<Long, Long> getItemCountsByQuestionForm(Long subjectId) {
        log.info("[인덱스 없음] 문제 형식별 문항 수 집계: subjectId={}", subjectId);
        return itemMetadataRepository.countItemsByQuestionForm(subjectId);
    }

    @Override
    public Map<Long, Long> getItemCountsBySubjects(List<Long> subjectIds) {
        return itemMetadataRepository.countItemsBySubjects(subjectIds);
    }

    @Override
    public List<ItemSearchResponse> getItemsByPassage(Long passageId) {
        List<ItemMetadata> items = itemMetadataRepository.findByPassageId(passageId);

        return items.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemSearchResponse> getItemsByIds(List<Long> itemIds) {
        List<ItemMetadata> items = itemMetadataRepository.findAllById(itemIds);

        return items.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Entity를 Response DTO로 변환
     */
    private ItemSearchResponse convertToResponse(ItemMetadata metadata) {
        ItemSearchResponse.ItemSearchResponseBuilder builder = ItemSearchResponse.builder()
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

        // 이미지 데이터 처리
        if (metadata.getHasImageData() && metadata.getImageData() != null) {
            ItemImageData imageData = metadata.getImageData();
            builder.questionImageUrl(imageData.getQuestionUrl())
                    .answerImageUrl(imageData.getAnswerUrl())
                    .explainImageUrl(imageData.getExplainUrl())
                    .passageImageUrl(imageData.getPassageUrl());
        }

        // HTML 데이터 처리 (필요시)
        if (metadata.getHasHtmlData() && metadata.getHtmlData() != null) {
            ItemHtmlData htmlData = metadata.getHtmlData();
            builder.questionHtml(htmlData.getQuestionHtml())
                    .answerHtml(htmlData.getAnswerHtml())
                    .explainHtml(htmlData.getExplainHtml())
                    .passageHtml(htmlData.getPassageHtml())
                    .choice1Html(htmlData.getChoice1Html())
                    .choice2Html(htmlData.getChoice2Html())
                    .choice3Html(htmlData.getChoice3Html())
                    .choice4Html(htmlData.getChoice4Html())
                    .choice5Html(htmlData.getChoice5Html());
        }

        return builder.build();
    }
}

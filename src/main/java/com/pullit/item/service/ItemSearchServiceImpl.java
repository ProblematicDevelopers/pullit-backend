package com.pullit.item.service;

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
    public Page<ItemSearchResponse> searchItems(ItemSearchRequest request) {
        log.debug("문항 검색 요청:{}",request);

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        Page<ItemMetadata> itemPage = itemMetadataRepository.searchItems(request, pageable);

        return itemPage.map(this::convertToResponse);
    }

    @Override
    public ItemSearchResponse getItemDetail(Long itemId) {
        ItemMetadata metadata = itemMetadataRepository.findByItemId(itemId)
                .orElseThrow(() -> new EntityNotFoundException("문항을 찾을 수 없습니다: " + itemId));

        return convertToResponse(metadata);
    }

    @Override
    @Cacheable(value = "chapterItemCounts", key = "#subjectId + '-' + #chapterIds.hashCode()")
    public Map<Long, Long> getItemCountsByChapters(Long subjectId, List<Long> chapterIds) {
        return itemMetadataRepository.countItemsByChapters(subjectId, chapterIds);
    }

    @Override
    @Cacheable(value = "difficultyItemCounts", key = "#subjectId")
    public Map<Long, Long> getItemCountsByDifficulty(Long subjectId) {
        return itemMetadataRepository.countItemsByDifficulty(subjectId);
    }

    @Override
    @Cacheable(value = "questionFormItemCounts", key = "#subjectId")
    public Map<Long, Long> getItemCountsByQuestionForm(Long subjectId) {
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
                    .passageHtml(htmlData.getPassageHtml());
        }

        return builder.build();
    }
}

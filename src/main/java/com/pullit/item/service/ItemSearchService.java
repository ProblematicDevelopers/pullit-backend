package com.pullit.item.service;

import com.pullit.item.dto.request.ItemSearchRequest;
import com.pullit.item.dto.response.ItemSearchResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface ItemSearchService {


    Page<ItemSearchResponse> searchItems(ItemSearchRequest request);

    ItemSearchResponse getItemDetail(Long itemId);

    Map<Long, Long> getItemCountsByChapters(Long subjectId, List<Long> chapterIds);

    Map<Long, Long> getItemCountsByDifficulty(Long subjectId);

    Map<Long, Long> getItemCountsByQuestionForm(Long subjectId);

    Map<Long, Long> getItemCountsBySubjects(List<Long> subjectIds);

    List<ItemSearchResponse> getItemsByPassage(Long passageId);

    List<ItemSearchResponse> getItemsByIds(List<Long> itemIds);

}

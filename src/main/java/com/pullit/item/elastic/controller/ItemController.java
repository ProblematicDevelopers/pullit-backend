package com.pullit.item.elastic.controller;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.item.elastic.document.ItemImageDocument;
import com.pullit.item.elastic.service.ItemImageService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/es/items")
public class ItemController {

    private final ItemImageService itemImageService;

    public ItemController(ItemImageService itemImageService) {
        this.itemImageService = itemImageService;
    }

    @PostMapping("/similar")
    public ResponseEntity<ApiResponse<List<ItemImageDocument>>> getSimilarItems(
            @RequestBody SimilarItemsRequest request
    ) throws IOException {
        List<ItemImageDocument> similarItems = itemImageService.findSimilarItems(
                request.getTopicChapterId(),
                request.getDifficultyCode(),
                request.getExcludeItemIds(),
                request.getSize()
        );
        return ResponseEntity.ok(ApiResponse.success(similarItems, "유사 문항 조회 성공"));
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimilarItemsRequest {
        private long topicChapterId;
        private int difficultyCode;
        private List<Long> excludeItemIds = new ArrayList<>();
        private int size = 20;
    }
}

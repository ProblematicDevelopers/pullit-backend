package com.pullit.item.elastic.controller;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.item.elastic.document.ItemImageDocument;
import com.pullit.item.elastic.service.ItemImageService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/items")
public class ItemController {

    private final ItemImageService itemImageService;

    public ItemController(ItemImageService itemImageService) {
        this.itemImageService = itemImageService;
    }

    @Operation(summary = "유사 문항 조회", description = "topicChapterId, difficultyCode, excludeItemIds(제외할 itemId 리스트), size(조회할 문항수 default:20)")
    @PostMapping("/similar")
    public ResponseEntity<ApiResponse<List<ItemImageDocument>>> getSimilarItems(
            @RequestBody SimilarItemsRequest request
    ) throws IOException {
        List<ItemImageDocument> similarItems = itemImageService.findSimilarItems(
                request.getTopicChapterId(),
                request.getDifficultyCode(),
                request.getPassageId(),
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
        private int difficultyCode = -1;
        private long passageId = -1;
        private List<Long> excludeItemIds = new ArrayList<>();
        private int size = 20;
    }
}

package com.pullit.item.elastic.controller;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.item.dto.request.SmartSelectionRequest;
import com.pullit.item.dto.response.SmartSelectionResponse;
import com.pullit.item.elastic.document.ItemImageDocument;
import com.pullit.item.elastic.service.ItemImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/items")
@Slf4j
@Tag(name="Elastic Search", description = "ES 검색")
public class ItemController {

    private final ItemImageService itemImageService;

    public ItemController(ItemImageService itemImageService) {
        this.itemImageService = itemImageService;
    }

    @Operation(summary = "랜덤 문항 조회", description = "랜덤 문항 조회 기능")
    @PostMapping("/random")
    public ResponseEntity<ApiResponse<SmartSelectionResponse>> getRandomItems(
            @RequestBody @Valid SmartSelectionRequest request
    ) throws IOException {
        log.info("스마트 문항 선택 요청 - 교과서: {}, 문항수: {}, 난이도: {}",
                request.getSubjectId(), request.getItemCount(), request.getDifficulty());

        SmartSelectionResponse response = itemImageService.smartSelectItems(request);

        return ResponseEntity.ok(ApiResponse.success(response, "스마트 문항 선택 성공"));
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

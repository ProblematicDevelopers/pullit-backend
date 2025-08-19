package com.pullit.item.controller;

import com.pullit.common.dto.response.ApiResponse;
import com.pullit.item.dto.request.ItemSearchRequest;
import com.pullit.item.dto.response.ItemSearchResponse;
import com.pullit.item.service.ItemSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@Tag(name="Item Search", description = "문항 검색 API")
public class ItemSearchController {
    private final ItemSearchService itemSearchService;
    private final com.pullit.item.elastic.controller.ItemController itemController;

    @Operation(summary = "문항 검색", description = "교과서별 문항을 검색합니다")
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Page<ItemSearchResponse>>> searchItems(@RequestBody @Valid ItemSearchRequest request) {
        log.info("문항 검색 요청 - 교과서ID: {}, 페이지: {}", request.getSubjectId(), request.getPage());

        Page<ItemSearchResponse> result = itemSearchService.searchItems(request);
        return ResponseEntity.ok(ApiResponse.success(result,"문항 검색 성공"));
    }

    @Operation(summary = "문항 상세 조회", description = "특정 문항의 상세 정보를 조회합니다")
    @GetMapping("/{itemId}")
    public ResponseEntity<ApiResponse<ItemSearchResponse>> getItemDetail(@Parameter(description = "문항 ID") @PathVariable Long itemId) {
        log.info("문항 상세 조회 {}", itemId);

        ItemSearchResponse result = itemSearchService.getItemDetail(itemId);

        return ResponseEntity.ok(ApiResponse.success(result,"문항 조회 성공"));

    }
    @Operation(summary = "챕터별 문항 수 조회", description = "교과서의 챕터별 문항 개수를 조회합니다")
    @GetMapping("/count/chapters")
    public ResponseEntity<ApiResponse<Map<Long, Long>>> getChapterItemCounts(
            @Parameter(description = "교과서 ID") @RequestParam Long subjectId,
            @Parameter(description = "챕터 ID 목록") @RequestParam List<Long> chapterIds) {

        Map<Long, Long> counts = itemSearchService.getItemCountsByChapters(subjectId, chapterIds);

        return ResponseEntity.ok(ApiResponse.success(counts, "챕터별 문항 수 조회 성공"));
    }

    @Operation(summary = "난이도별 문항 수 조회", description = "교과서의 난이도별 문항 개수를 조회합니다")
    @GetMapping("/count/difficulty")
    public ResponseEntity<ApiResponse<Map<Long, Long>>> getDifficultyItemCounts(
            @Parameter(description = "교과서 ID") @RequestParam Long subjectId) {

        Map<Long, Long> counts = itemSearchService.getItemCountsByDifficulty(subjectId);

        return ResponseEntity.ok(ApiResponse.success(counts, "난이도별 문항 수 조회 성공"));
    }

    @Operation(summary = "문제 유형별 문항 수 조회", description = "교과서의 문제 유형별 문항 개수를 조회합니다")
    @GetMapping("/count/question-form")
    public ResponseEntity<ApiResponse<Map<Long, Long>>> getQuestionFormItemCounts(
            @Parameter(description = "교과서 ID") @RequestParam Long subjectId) {

        Map<Long, Long> counts = itemSearchService.getItemCountsByQuestionForm(subjectId);

        return ResponseEntity.ok(ApiResponse.success(counts, "문제 유형별 문항 수 조회 성공"));
    }

    @Operation(summary = "교과서별 문항 수 조회", description = "여러 교과서의 문항 개수를 조회합니다")
    @PostMapping("/count/subjects")
    public ResponseEntity<ApiResponse<Map<Long, Long>>> getSubjectItemCounts(
            @RequestBody List<Long> subjectIds) {

        Map<Long, Long> counts = itemSearchService.getItemCountsBySubjects(subjectIds);

        return ResponseEntity.ok(ApiResponse.success(counts, "교과서별 문항 수 조회 성공"));
    }

    @Operation(summary = "지문별 문항 조회", description = "특정 지문에 연결된 문항들을 조회합니다")
    @GetMapping("/passage/{passageId}")
    public ResponseEntity<ApiResponse<List<ItemSearchResponse>>> getItemsByPassage(
            @Parameter(description = "지문 ID") @PathVariable Long passageId) {

        List<ItemSearchResponse> items = itemSearchService.getItemsByPassage(passageId);

        return ResponseEntity.ok(ApiResponse.success(items, "지문별 문항 조회 성공"));
    }

    @Operation(summary = "여러 문항 일괄 조회", description = "문항 ID 목록으로 여러 문항을 한번에 조회합니다")
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<ItemSearchResponse>>> getItemsByIds(
            @RequestBody List<Long> itemIds) {

        log.info("문항 일괄 조회 - 개수: {}", itemIds.size());

        List<ItemSearchResponse> items = itemSearchService.getItemsByIds(itemIds);

        return ResponseEntity.ok(ApiResponse.success(items, "문항 일괄 조회 성공"));
    }

    @Operation(summary = "유사 문항 조회", description = "ES를 통한 유사 문항 검색")
    @PostMapping("/similar")
    public ResponseEntity<?> getSimilarItems(
            @RequestBody com.pullit.item.elastic.controller.ItemController.SimilarItemsRequest request
    ) throws IOException {
        // ES ItemController의 메서드를 그대로 호출
        return itemController.getSimilarItems(request);
    }

}

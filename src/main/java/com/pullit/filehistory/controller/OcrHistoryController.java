package com.pullit.filehistory.controller;

import com.pullit.common.constants.ServiceConstants;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.filehistory.dto.request.OcrHistoryBulkSaveRequest;
import com.pullit.filehistory.entity.OcrHistory;
import com.pullit.filehistory.service.OcrHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "OCR History API", description = "OCR 히스토리 관리 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/ocr-history")
public class OcrHistoryController {
    
    private final OcrHistoryService ocrHistoryService;
    
    @Operation(
            summary = "OCR 영역 일괄 저장",
            description = "영역 선택 완료 후 좌표와 메타데이터를 일괄 저장합니다."
    )
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<Long>>> bulkSaveOcrHistories(
            @RequestBody OcrHistoryBulkSaveRequest request) {
        
        log.info("OCR 히스토리 일괄 저장 요청 - processedItemId: {}, pdfImageId: {}, areas: {}", 
                request.getProcessedItemId(), request.getPdfImageId(), request.getAreas().size());
        
        List<Long> savedIds = ocrHistoryService.bulkSaveOcrHistories(request);
        
        return ResponseEntity.ok(ApiResponse.success(savedIds, "OCR 히스토리 일괄 저장 완료"));
    }
    
    @Operation(
            summary = "ProcessedItem의 OCR 히스토리 조회",
            description = "특정 ProcessedItem에 속한 모든 OCR 히스토리를 조회합니다."
    )
    @GetMapping("/processed-item/{processedItemId}")
    public ResponseEntity<ApiResponse<List<OcrHistory>>> getOcrHistoriesByProcessedItemId(
            @Parameter(description = "ProcessedItem ID", required = true)
            @PathVariable Long processedItemId) {
        
        List<OcrHistory> histories = ocrHistoryService.getOcrHistoriesByProcessedItemId(processedItemId);
        
        return ResponseEntity.ok(ApiResponse.success(histories, "OCR 히스토리 조회 완료"));
    }
    
    @Operation(
            summary = "PdfImage의 OCR 히스토리 조회",
            description = "특정 PDF 페이지(PdfImage)의 모든 OCR 히스토리를 조회합니다."
    )
    @GetMapping("/pdf-image/{pdfImageId}")
    public ResponseEntity<ApiResponse<List<OcrHistory>>> getOcrHistoriesByPdfImageId(
            @Parameter(description = "PdfImage ID", required = true)
            @PathVariable Long pdfImageId) {
        
        List<OcrHistory> histories = ocrHistoryService.getOcrHistoriesByPdfImageId(pdfImageId);
        
        return ResponseEntity.ok(ApiResponse.success(histories, "OCR 히스토리 조회 완료"));
    }
    
    @Operation(
            summary = "임시 OCR 히스토리 정리",
            description = "영역 선택만 하고 모달에서 나간 경우, processedItemId가 null인 임시 데이터를 삭제합니다."
    )
    @DeleteMapping("/temporary/{pdfImageId}")
    public ResponseEntity<ApiResponse<Integer>> cleanupTemporaryOcrHistories(
            @Parameter(description = "PdfImage ID", required = true)
            @PathVariable Long pdfImageId) {
        
        log.info("임시 OCR 히스토리 정리 요청 - pdfImageId: {}", pdfImageId);
        
        int deletedCount = ocrHistoryService.cleanupTemporaryOcrHistories(pdfImageId);
        
        return ResponseEntity.ok(ApiResponse.success(deletedCount, "임시 OCR 히스토리 정리 완료"));
    }
    
    @Operation(
            summary = "OCR 히스토리 확정 저장",
            description = "임시 OCR 히스토리를 ProcessedItem과 연결하여 영구 보존합니다."
    )
    @PutMapping("/confirm/{pdfImageId}/{processedItemId}")
    public ResponseEntity<ApiResponse<Integer>> confirmOcrHistories(
            @Parameter(description = "PdfImage ID", required = true)
            @PathVariable Long pdfImageId,
            @Parameter(description = "ProcessedItem ID", required = true)
            @PathVariable Long processedItemId) {
        
        log.info("OCR 히스토리 확정 저장 요청 - pdfImageId: {}, processedItemId: {}", pdfImageId, processedItemId);
        
        int confirmedCount = ocrHistoryService.confirmOcrHistories(pdfImageId, processedItemId);
        
        return ResponseEntity.ok(ApiResponse.success(confirmedCount, "OCR 히스토리 확정 저장 완료"));
    }
    
    @Operation(
            summary = "임시 OCR 히스토리 조회",
            description = "특정 PDF 페이지의 임시 OCR 히스토리(processedItemId가 null)를 조회합니다."
    )
    @GetMapping("/temporary/{pdfImageId}")
    public ResponseEntity<ApiResponse<List<OcrHistory>>> getTemporaryOcrHistories(
            @Parameter(description = "PdfImage ID", required = true)
            @PathVariable Long pdfImageId) {
        
        List<OcrHistory> temporaryHistories = ocrHistoryService.getTemporaryOcrHistories(pdfImageId);
        
        return ResponseEntity.ok(ApiResponse.success(temporaryHistories, "임시 OCR 히스토리 조회 완료"));
    }
}
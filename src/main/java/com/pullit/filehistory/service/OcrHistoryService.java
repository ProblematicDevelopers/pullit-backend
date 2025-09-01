package com.pullit.filehistory.service;

import com.pullit.filehistory.dto.request.OcrHistoryBulkSaveRequest;
import com.pullit.filehistory.entity.BoundingBox;
import com.pullit.filehistory.entity.OcrHistory;
import com.pullit.filehistory.entity.PdfImage;
import com.pullit.filehistory.repository.OcrHistoryRepository;
import com.pullit.filehistory.repository.PdfImageRepository;
import com.pullit.itemprocess.entity.ProcessedItem;
import com.pullit.itemprocess.repository.ProcessedItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OcrHistoryService {
    
    private final OcrHistoryRepository ocrHistoryRepository;
    private final PdfImageRepository pdfImageRepository;
    private final ProcessedItemRepository processedItemRepository;
    
    /**
     * 영역 선택 완료 후 좌표와 메타데이터를 일괄 저장
     */
    public List<Long> bulkSaveOcrHistories(OcrHistoryBulkSaveRequest request) {
        log.info("OCR 히스토리 일괄 저장 시작 - processedItemId: {}, pdfImageId: {}, areas: {}", 
                request.getProcessedItemId(), request.getPdfImageId(), request.getAreas().size());
        
        // 필수 엔티티 조회
        PdfImage pdfImage = pdfImageRepository.findById(request.getPdfImageId())
                .orElseThrow(() -> new IllegalArgumentException("PdfImage not found: " + request.getPdfImageId()));
        
        ProcessedItem processedItem = null;
        if (request.getProcessedItemId() != null) {
            processedItem = processedItemRepository.findById(request.getProcessedItemId())
                    .orElseThrow(() -> new IllegalArgumentException("ProcessedItem not found: " + request.getProcessedItemId()));
        }
        
        List<Long> savedIds = new ArrayList<>();
        
        for (OcrHistoryBulkSaveRequest.AreaData areaData : request.getAreas()) {
            // 기존 히스토리가 있는지 확인 (idempotent)
            OcrHistory existingHistory = null;
            if (processedItem != null) {
                existingHistory = ocrHistoryRepository.findByProcessedItemIdAndAreaType(
                        processedItem.getId(), areaData.getAreaType());
            }
            
            // BoundingBox 생성
            BoundingBox boundingBox = BoundingBox.builder()
                    .normalizedX(areaData.getX())
                    .normalizedY(areaData.getY())
                    .normalizedWidth(areaData.getWidth())
                    .normalizedHeight(areaData.getHeight())
                    .pageNo(areaData.getPageNo())
                    .scale(areaData.getScale())
                    .rotation(areaData.getRotation())
                    .canvasWidth(areaData.getCanvasWidth())
                    .canvasHeight(areaData.getCanvasHeight())
                    .pixelX(areaData.getPixelX())
                    .pixelY(areaData.getPixelY())
                    .pixelWidth(areaData.getPixelWidth())
                    .pixelHeight(areaData.getPixelHeight())
                    .build();
            
            OcrHistory ocrHistory;
            if (existingHistory != null) {
                // 업데이트
                existingHistory.setBoundingBox(boundingBox);
                existingHistory.setOriginalImageUrl(areaData.getOriginalImageUrl());
                if (areaData.getOcrText() != null) {
                    existingHistory.setOcrText(areaData.getOcrText());
                }
                // 기존 좌표 필드도 호환성 위해 업데이트
                existingHistory.setPositionX(areaData.getPixelX() != null ? areaData.getPixelX().toString() : "0");
                existingHistory.setPositionY(areaData.getPixelY() != null ? areaData.getPixelY().toString() : "0");
                existingHistory.setSizeX(areaData.getPixelWidth() != null ? areaData.getPixelWidth().toString() : "0");
                existingHistory.setSizeY(areaData.getPixelHeight() != null ? areaData.getPixelHeight().toString() : "0");
                
                ocrHistory = ocrHistoryRepository.save(existingHistory);
                log.debug("기존 OCR 히스토리 업데이트: id={}, areaType={}", existingHistory.getId(), areaData.getAreaType());
            } else {
                // 새로 생성
                ocrHistory = OcrHistory.builder()
                        .pdfImage(pdfImage)
                        .processedItem(processedItem)
                        .areaType(areaData.getAreaType())
                        .boundingBox(boundingBox)
                        .originalImageUrl(areaData.getOriginalImageUrl())
                        .ocrText(areaData.getOcrText())
                        // 기존 좌표 필드도 호환성 위해 설정
                        .positionX(areaData.getPixelX() != null ? areaData.getPixelX().toString() : "0")
                        .positionY(areaData.getPixelY() != null ? areaData.getPixelY().toString() : "0")
                        .sizeX(areaData.getPixelWidth() != null ? areaData.getPixelWidth().toString() : "0")
                        .sizeY(areaData.getPixelHeight() != null ? areaData.getPixelHeight().toString() : "0")
                        .build();
                
                ocrHistory = ocrHistoryRepository.save(ocrHistory);
                log.debug("새 OCR 히스토리 생성: id={}, areaType={}", ocrHistory.getId(), areaData.getAreaType());
            }
            
            savedIds.add(ocrHistory.getId());
        }
        
        log.info("OCR 히스토리 일괄 저장 완료 - 총 {}건 처리", savedIds.size());
        return savedIds;
    }
    
    /**
     * ProcessedItem ID로 모든 OCR 히스토리 조회
     */
    @Transactional(readOnly = true)
    public List<OcrHistory> getOcrHistoriesByProcessedItemId(Long processedItemId) {
        return ocrHistoryRepository.findByProcessedItemId(processedItemId);
    }
    
    /**
     * PdfImage ID로 모든 OCR 히스토리 조회
     */
    @Transactional(readOnly = true)
    public List<OcrHistory> getOcrHistoriesByPdfImageId(Long pdfImageId) {
        return ocrHistoryRepository.findByPdfImageId(pdfImageId);
    }
    
    /**
     * 임시 OCR 히스토리 정리 (processedItemId가 null인 것들)
     * 영역 선택만 하고 모달에서 나간 경우 호출
     */
    public int cleanupTemporaryOcrHistories(Long pdfImageId) {
        log.info("임시 OCR 히스토리 정리 시작 - pdfImageId: {}", pdfImageId);
        
        List<OcrHistory> temporaryHistories = ocrHistoryRepository.findByPdfImageIdAndProcessedItemIsNull(pdfImageId);
        
        if (!temporaryHistories.isEmpty()) {
            ocrHistoryRepository.deleteAll(temporaryHistories);
            log.info("임시 OCR 히스토리 정리 완료 - 삭제된 항목: {}개", temporaryHistories.size());
            return temporaryHistories.size();
        }
        
        log.info("정리할 임시 OCR 히스토리가 없습니다 - pdfImageId: {}", pdfImageId);
        return 0;
    }
    
    /**
     * 임시 OCR 히스토리를 ProcessedItem과 연결하여 확정 저장
     * ProcessedItem 저장 완료 후 호출
     */
    public int confirmOcrHistories(Long pdfImageId, Long processedItemId) {
        log.info("OCR 히스토리 확정 저장 시작 - pdfImageId: {}, processedItemId: {}", pdfImageId, processedItemId);
        
        // ProcessedItem 조회
        ProcessedItem processedItem = processedItemRepository.findById(processedItemId)
                .orElseThrow(() -> new IllegalArgumentException("ProcessedItem not found: " + processedItemId));
        
        // 임시 OCR 히스토리들 조회 및 업데이트
        List<OcrHistory> temporaryHistories = ocrHistoryRepository.findByPdfImageIdAndProcessedItemIsNull(pdfImageId);
        
        int updatedCount = 0;
        for (OcrHistory history : temporaryHistories) {
            history.setProcessedItem(processedItem);
            ocrHistoryRepository.save(history);
            updatedCount++;
        }
        
        log.info("OCR 히스토리 확정 저장 완료 - 업데이트된 항목: {}개", updatedCount);
        return updatedCount;
    }
    
    /**
     * 임시 OCR 히스토리 조회 (processedItemId가 null인 것들)
     */
    @Transactional(readOnly = true)
    public List<OcrHistory> getTemporaryOcrHistories(Long pdfImageId) {
        return ocrHistoryRepository.findByPdfImageIdAndProcessedItemIsNull(pdfImageId);
    }
}
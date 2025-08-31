package com.pullit.itemprocess.service;

import com.pullit.filehistory.entity.OcrHistory;
import com.pullit.filehistory.entity.PdfImage;
import com.pullit.filehistory.repository.OcrHistoryRepository;
import com.pullit.filehistory.repository.PdfImageRepository;
import com.pullit.itemprocess.dto.request.ProcessedItemSaveRequest;
import com.pullit.itemprocess.entity.ProcessedItem;
import com.pullit.itemprocess.repository.ProcessedItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProcessedItemService {
    
    private final ProcessedItemRepository processedItemRepository;
    private final OcrHistoryRepository ocrHistoryRepository;
    private final PdfImageRepository pdfImageRepository;
    
    public ProcessedItem saveProcessedItem(ProcessedItemSaveRequest request) {
        ProcessedItem processedItem = ProcessedItem.builder()
                .type(request.getType())
                .difficulty(request.getDifficulty())
                .answer(request.getAnswer())
                .score(request.getScore())
                .majorChapterId(request.getMajorChapterId())
                .middleChapterId(request.getMiddleChapterId())
                .minorChapterId(request.getMinorChapterId())
                .solution(request.getSolution())
                .explanation(request.getExplanation())
                .passageId(request.getPassageId())
                .build();
        
        ProcessedItem savedItem = processedItemRepository.save(processedItem);
        
        if (request.getOcrHistories() != null) {
            for (ProcessedItemSaveRequest.OcrHistoryData ocrData : request.getOcrHistories()) {
                PdfImage pdfImage = pdfImageRepository.findById(ocrData.getPdfImageId())
                        .orElseThrow(() -> new IllegalArgumentException("PdfImage not found: " + ocrData.getPdfImageId()));
                
                OcrHistory ocrHistory = OcrHistory.builder()
                        .pdfImage(pdfImage)
                        .processedItem(savedItem)
                        .areaType(ocrData.getAreaType())
                        .ocrText(ocrData.getOcrText())
                        .editedText(ocrData.getEditedText())
                        .originalImageUrl(ocrData.getOriginalImageUrl())
                        .positionX(ocrData.getPositionX())
                        .positionY(ocrData.getPositionY())
                        .sizeX(ocrData.getSizeX())
                        .sizeY(ocrData.getSizeY())
                        .build();
                
                ocrHistoryRepository.save(ocrHistory);
                savedItem.addOcrHistory(ocrHistory);
            }
        }
        
        return savedItem;
    }
    
    /**
     * 저장된 처리된 문항들을 페이지네이션으로 조회
     */
    @Transactional(readOnly = true)
    public Page<ProcessedItem> getProcessedItems(Pageable pageable, String subjectCode) {
        log.info("저장된 문항 목록 조회 - page: {}, size: {}, subjectCode: {}", 
                pageable.getPageNumber(), pageable.getPageSize(), subjectCode);
        
        if (subjectCode != null && !subjectCode.isEmpty()) {
            // 과목 코드로 필터링 (필요시 추후 구현)
            return processedItemRepository.findAll(pageable);
        } else {
            return processedItemRepository.findAll(pageable);
        }
    }
    
    /**
     * ID로 특정 처리된 문항의 상세 정보 조회
     */
    @Transactional(readOnly = true)
    public ProcessedItem getProcessedItemById(Long id) {
        log.info("문항 상세 조회 - id: {}", id);
        return processedItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ProcessedItem not found: " + id));
    }
    
    /**
     * 파일 ID로 해당 파일의 모든 OCR 처리 히스토리 조회
     */
    @Transactional(readOnly = true)
    public List<OcrHistory> getOcrHistoryByFileId(Long fileId) {
        log.info("OCR 히스토리 조회 - fileId: {}", fileId);
        return ocrHistoryRepository.findByPdfImageId(fileId);
    }
    
    /**
     * 파일 ID로 해당 파일에서 완료된 OCR 처리 영역들을 위치 정보와 함께 조회
     */
    @Transactional(readOnly = true)
    public List<OcrHistory> getCompletedOcrRegionsByFileId(Long fileId) {
        log.info("완료된 OCR 영역 조회 - fileId: {}", fileId);
        // 처리 완료된 OCR 히스토리만 조회 (editedText가 있는 것들)
        return ocrHistoryRepository.findByPdfImageIdAndEditedTextIsNotNull(fileId);
    }
}
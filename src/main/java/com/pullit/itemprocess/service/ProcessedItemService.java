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
}
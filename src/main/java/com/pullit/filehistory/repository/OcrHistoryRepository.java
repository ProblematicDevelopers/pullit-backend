package com.pullit.filehistory.repository;

import com.pullit.filehistory.entity.OcrHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OcrHistoryRepository extends JpaRepository<OcrHistory, Long> {
    
    /**
     * 특정 PDF 이미지 ID로 OCR 히스토리 조회
     */
    List<OcrHistory> findByPdfImageId(Long pdfImageId);
    
    /**
     * 특정 PDF 이미지 ID로 완료된 OCR 히스토리 조회 (editedText가 있는 것들)
     */
    List<OcrHistory> findByPdfImageIdAndEditedTextIsNotNull(Long pdfImageId);

    List<OcrHistory> findByProcessedItemId(Long processedItemId);
    
    /**
     * ProcessedItem ID와 AreaType으로 특정 OCR 히스토리 조회 (idempotent 처리용)
     */
    OcrHistory findByProcessedItemIdAndAreaType(Long processedItemId, com.pullit.itemprocess.enums.AreaType areaType);
    
    /**
     * 특정 PDF 이미지의 임시 OCR 히스토리 조회 (processedItem이 null인 것들)
     */
    List<OcrHistory> findByPdfImageIdAndProcessedItemIsNull(Long pdfImageId);
}
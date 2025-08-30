package com.pullit.filehistory.service;

import com.pullit.common.exception.BusinessException;
import com.pullit.filehistory.dto.response.PdfProcessingResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface PdfProcessingService {
    
    /**
     * PDF 파일을 이미지로 변환하고 S3에 업로드
     * @param pdfFile PDF 파일
     * @param fileHistoryId 파일 히스토리 ID
     * @return PDF 처리 결과 (이미지 URL 목록 포함)
     */
    PdfProcessingResponse processPdfToImages(MultipartFile pdfFile, Long fileHistoryId) throws BusinessException;
    
    /**
     * PDF 페이지 순서 변경 및 삭제 처리
     * @param fileHistoryId 파일 히스토리 ID
     * @param imageOrder 이미지 순서 (콤마로 구분된 인덱스)
     * @return 업데이트된 이미지 목록
     */
    List<String> updateImageOrder(Long fileHistoryId, String imageOrder) throws BusinessException;;
    
    /**
     * 특정 페이지 삭제
     * @param fileHistoryId 파일 히스토리 ID
     * @param pageIndex 삭제할 페이지 인덱스
     * @return 업데이트된 이미지 목록
     */
    List<String> removePage(Long fileHistoryId, int pageIndex) throws BusinessException;;
}
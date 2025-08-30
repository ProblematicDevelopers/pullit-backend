package com.pullit.filehistory.service;

import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;
import com.pullit.common.s3.service.S3Service;
import com.pullit.common.s3.enums.S3Directory;
import com.pullit.filehistory.dto.response.PdfProcessingResponse;
import com.pullit.filehistory.entity.FileHistory;
import com.pullit.filehistory.entity.PdfImage;
import com.pullit.filehistory.repository.FileHistoryRepository;
import com.pullit.filehistory.repository.PdfImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfProcessingServiceImpl implements PdfProcessingService {

    private final S3Service s3Service;
    private final FileHistoryRepository fileHistoryRepository;
    private final PdfImageRepository pdfImageRepository;

    @Value("${app.pdf.image-dpi:300}")
    private int imageDpi;

    @Value("${app.pdf.image-format:png}")
    private String imageFormat;

    @Override
    @Transactional
    public PdfProcessingResponse processPdfToImages(MultipartFile pdfFile, Long fileHistoryId) throws BusinessException {
        log.info("Starting PDF processing: fileHistoryId={}, fileName={}", fileHistoryId, pdfFile.getOriginalFilename());
        
        try {
            // FileHistory 조회
            FileHistory fileHistory = fileHistoryRepository.findById(fileHistoryId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

            List<PdfProcessingResponse.PdfImageInfo> imageInfos = new ArrayList<>();
            // PDF 문서 로드
            byte[] pdfBytes = pdfFile.getBytes();
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                PDFRenderer pdfRenderer = new PDFRenderer(document);
                int pageCount = document.getNumberOfPages();
                
                log.info("PDF loaded successfully. Total pages: {}", pageCount);
                
                for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                    // PDF 페이지를 이미지로 렌더링
                    float scale = imageDpi / 72f; // 72 DPI가 기본값
                    BufferedImage bufferedImage = pdfRenderer.renderImage(pageIndex, scale);
                    
                    // 이미지를 바이트 배열로 변환
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(bufferedImage, imageFormat, baos);
                    byte[] imageBytes = baos.toByteArray();
                    
                    // S3에 업로드
                    String fileName = String.format("page_%d_%d.%s", 
                            fileHistoryId, pageIndex + 1, imageFormat);
                    var uploadResponse = s3Service.upload(imageBytes, fileName, S3Directory.IMAGE_QUESTION);
                    String s3Key = uploadResponse.getS3Key();
                    
                    // Presigned URL 생성 (1시간 유효)
                    String presignedUrl = s3Service.generatePresignedUrl(s3Key);
                    
                    // PdfImage 엔티티 생성 및 저장
                    PdfImage pdfImage = PdfImage.builder()
                            .fileHistory(fileHistory)
                            .pageNumber(pageIndex + 1)
                            .imageUrl(presignedUrl)
                            .imageWidth(bufferedImage.getWidth())
                            .imageHeight(bufferedImage.getHeight())
                            .fileSize((long) imageBytes.length)
                            .s3Key(s3Key)
                            .build();
                    
                    pdfImage = pdfImageRepository.save(pdfImage);
                    
                    // 응답 DTO 생성
                    PdfProcessingResponse.PdfImageInfo imageInfo = PdfProcessingResponse.PdfImageInfo.builder()
                            .pdfImageId(pdfImage.getId())
                            .pageNumber(pageIndex + 1)
                            .imageUrl(presignedUrl)
                            .width(bufferedImage.getWidth())
                            .height(bufferedImage.getHeight())
                            .fileSize((long) imageBytes.length)
                            .build();
                    
                    imageInfos.add(imageInfo);
                    
                    log.debug("Page {} processed successfully. Image size: {}x{}, File size: {} bytes", 
                            pageIndex + 1, bufferedImage.getWidth(), bufferedImage.getHeight(), imageBytes.length);
                }
                
                // FileHistory의 이미지 개수 업데이트
                fileHistory.setImgCount(pageCount);
                fileHistoryRepository.save(fileHistory);
                
                log.info("PDF processing completed successfully. Total pages processed: {}", pageCount);
                
                return PdfProcessingResponse.builder()
                        .fileHistoryId(fileHistoryId)
                        .totalPages(pageCount)
                        .images(imageInfos)
                        .message("PDF processing completed successfully")
                        .build();
                        
            }
            
        } catch (IOException e) {
            log.error("Error processing PDF file: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_PROCESSING_ERROR);
        }
    }

    @Override
    @Transactional
    public List<String> updateImageOrder(Long fileHistoryId, String imageOrder) throws BusinessException {
        log.info("Updating image order: fileHistoryId={}, order={}", fileHistoryId, imageOrder);
        
        FileHistory fileHistory = fileHistoryRepository.findById(fileHistoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
        
        // 기존 이미지 순서 업데이트
        fileHistory.setImgOrder(imageOrder);
        fileHistoryRepository.save(fileHistory);
        
        // 새로운 순서대로 이미지 URL 목록 반환
        List<PdfImage> pdfImages = pdfImageRepository.findByFileHistoryOrderByPageNumber(fileHistory);
        List<Integer> orderIndexes = Arrays.stream(imageOrder.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        
        List<String> reorderedUrls = new ArrayList<>();
        for (Integer index : orderIndexes) {
            if (index > 0 && index <= pdfImages.size()) {
                PdfImage pdfImage = pdfImages.get(index - 1); // 1-based to 0-based
                // 새로운 presigned URL 생성 (1시간 유효)
                String newPresignedUrl = s3Service.generatePresignedUrl(pdfImage.getS3Key(), Duration.ofHours(1));
                reorderedUrls.add(newPresignedUrl);
            }
        }
        
        return reorderedUrls;
    }

    @Override
    @Transactional
    public List<String> removePage(Long fileHistoryId, int pageIndex) throws BusinessException {
        log.info("Removing page: fileHistoryId={}, pageIndex={}", fileHistoryId, pageIndex);
        
        FileHistory fileHistory = fileHistoryRepository.findById(fileHistoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
        
        List<PdfImage> pdfImages = pdfImageRepository.findByFileHistoryOrderByPageNumber(fileHistory);
        
        if (pageIndex < 0 || pageIndex >= pdfImages.size()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        
        // 해당 페이지 삭제
        PdfImage imageToDelete = pdfImages.get(pageIndex);
        
        // S3에서 파일 삭제
        try {
            s3Service.delete(imageToDelete.getS3Key());
        } catch (Exception e) {
            log.warn("Failed to delete S3 file: {}", imageToDelete.getS3Key(), e);
        }
        
        // DB에서 삭제
        pdfImageRepository.delete(imageToDelete);
        
        // 이미지 개수 업데이트
        fileHistory.setImgCount(fileHistory.getImgCount() - 1);
        fileHistoryRepository.save(fileHistory);
        
        // 남은 이미지들의 새로운 presigned URL 목록 반환
        List<PdfImage> remainingImages = pdfImageRepository.findByFileHistoryOrderByPageNumber(fileHistory);
        return remainingImages.stream()
                .map(pdfImage -> s3Service.generatePresignedUrl(pdfImage.getS3Key(), Duration.ofHours(1)))
                .collect(Collectors.toList());
    }
}
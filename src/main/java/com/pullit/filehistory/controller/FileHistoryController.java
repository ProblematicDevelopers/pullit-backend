package com.pullit.filehistory.controller;

import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.common.annotation.AuthUser;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.filehistory.dto.response.PdfProcessingResponse;
import com.pullit.filehistory.service.FileHistoryService;
import com.pullit.filehistory.service.PdfProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/file-history")
@RequiredArgsConstructor
@Tag(name = "File History", description = "문제 가공 파일 히스토리 관련 API")
public class FileHistoryController {
    private final FileHistoryService fileHistoryService;
    private final PdfProcessingService pdfProcessingService;
    
    @Operation(summary = "파일 업로드 후 내역 저장", description = "file s3 저장 후 호출 api. id를 받아 파일 히스토리 저장")
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Long>> createFileHistory(@RequestParam Long fileMetadataId, @RequestParam Long subjectId, @AuthUser CustomUserDetails currentUser) {
        Long fileHistoryId = fileHistoryService.createHistory(fileMetadataId, subjectId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(fileHistoryId, "파일 히스토리 생성 완료"));
    }

    @Operation(summary = "PDF를 이미지로 변환", description = "업로드된 PDF 파일을 페이지별 이미지로 변환하여 S3에 저장")
    @PostMapping(value = "/process-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PdfProcessingResponse>> processPdfToImages(
            @Parameter(description = "PDF 파일", required = true)
            @RequestParam("file") MultipartFile pdfFile,
            @Parameter(description = "파일 히스토리 ID", required = true)
            @RequestParam("fileHistoryId") Long fileHistoryId,
            @AuthUser CustomUserDetails currentUser) {
        
        log.info("PDF processing request: fileHistoryId={}, fileName={}, size={}, userId={}", 
                fileHistoryId, pdfFile.getOriginalFilename(), pdfFile.getSize(), currentUser.getUserId());
        
        PdfProcessingResponse response = pdfProcessingService.processPdfToImages(pdfFile, fileHistoryId);
        
        return ResponseEntity.ok(ApiResponse.success(response, "PDF 처리가 완료되었습니다."));
    }

    @Operation(summary = "페이지 순서 변경", description = "PDF 이미지 페이지 순서를 변경합니다")
    @PutMapping("/update-order")
    public ResponseEntity<ApiResponse<List<String>>> updateImageOrder(
            @Parameter(description = "파일 히스토리 ID", required = true)
            @RequestParam("fileHistoryId") Long fileHistoryId,
            @Parameter(description = "이미지 순서 (콤마로 구분된 인덱스)", example = "1,3,2,4", required = true)
            @RequestParam("imageOrder") String imageOrder,
            @AuthUser CustomUserDetails currentUser) {
        
        log.info("Image order update request: fileHistoryId={}, order={}, userId={}", 
                fileHistoryId, imageOrder, currentUser.getUserId());
        
        List<String> updatedUrls = pdfProcessingService.updateImageOrder(fileHistoryId, imageOrder);
        
        return ResponseEntity.ok(ApiResponse.success(updatedUrls, "페이지 순서가 변경되었습니다."));
    }

    @Operation(summary = "페이지 삭제", description = "특정 PDF 이미지 페이지를 삭제합니다")
    @DeleteMapping("/remove-page")
    public ResponseEntity<ApiResponse<List<String>>> removePage(
            @Parameter(description = "파일 히스토리 ID", required = true)
            @RequestParam("fileHistoryId") Long fileHistoryId,
            @Parameter(description = "삭제할 페이지 인덱스 (0부터 시작)", required = true)
            @RequestParam("pageIndex") int pageIndex,
            @AuthUser CustomUserDetails currentUser) {
        
        log.info("Page removal request: fileHistoryId={}, pageIndex={}, userId={}", 
                fileHistoryId, pageIndex, currentUser.getUserId());
        
        List<String> remainingUrls = pdfProcessingService.removePage(fileHistoryId, pageIndex);
        
        return ResponseEntity.ok(ApiResponse.success(remainingUrls, "페이지가 삭제되었습니다."));
    }

}
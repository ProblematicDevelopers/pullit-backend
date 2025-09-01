package com.pullit.exam.controller;

import com.pullit.common.dto.response.ApiResponse;
import com.pullit.exam.dto.request.UserExamCreateRequest;
import com.pullit.exam.dto.response.UserExamResponse;
import com.pullit.exam.service.UserExamService;
import com.pullit.exam.service.UserExamServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

@Tag(name="UserExam", description = "사용자 시험지 관리 API")
@RestController
@RequestMapping("/api/user-exams")
@RequiredArgsConstructor
@Slf4j
public class UserExamController {
    
    private final UserExamServiceImpl userExamService;
    private final ObjectMapper objectMapper;
    
    @Operation(summary = "시험지 생성 (PDF 포함)", description = "시험지 정보와 PDF 파일을 함께 업로드하여 시험지를 생성합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserExamResponse>> createExamWithPDF(
            @RequestPart(value = "examData", required = false) String examDataJson,
            @RequestPart(value = "pdfFile", required = false) MultipartFile pdfFile) {
        
        log.info("시험지 생성 요청: examData={}, pdfSize={}", 
                examDataJson, pdfFile != null ? pdfFile.getSize() : 0);
        
        try {
            // JSON 문자열을 DTO로 변환
            UserExamCreateRequest request = objectMapper.readValue(examDataJson, UserExamCreateRequest.class);
            
            // 서비스 호출
            UserExamResponse response = userExamService.createExamWithPDF(request, pdfFile);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("시험지 생성 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("EXAM_CREATE_FAILED", "시험지 생성에 실패했습니다: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "시험지 생성 (JSON only)", description = "시험지 정보만으로 시험지를 생성합니다.")
    @PostMapping(value = "/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UserExamResponse>> createExam(
            @Valid @RequestBody UserExamCreateRequest request) {
        
        log.info("시험지 생성 요청 (JSON): examName={}", request.getExamName());
        
        UserExamResponse response = userExamService.createExam(request, null, null);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @Operation(summary = "시험지 조회", description = "시험지 ID로 시험지를 조회합니다.")
    @GetMapping("/{examId}")
    public ResponseEntity<ApiResponse<UserExamResponse>> getExam(@PathVariable Long examId) {
        log.info("시험지 조회 요청: examId={}", examId);
        
        UserExamResponse response = userExamService.getExam(examId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @Operation(summary = "시험지 PDF URL 업데이트", description = "기존 시험지의 PDF URL을 업데이트합니다.")
    @PutMapping("/{examId}/pdf-url")
    public ResponseEntity<ApiResponse<UserExamResponse>> updatePdfUrl(
            @PathVariable Long examId,
            @RequestParam String pdfUrl) {
        
        log.info("PDF URL 업데이트 요청: examId={}, pdfUrl={}", examId, pdfUrl);
        
        UserExamResponse response = userExamService.updatePdfUrl(examId, pdfUrl);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @Operation(summary = "시험지 삭제", description = "시험지를 삭제합니다. (Soft Delete)")
    @DeleteMapping("/{examId}")
    public ResponseEntity<ApiResponse<Void>> deleteExam(@PathVariable Long examId) {
        log.info("시험지 삭제 요청: examId={}", examId);
        
        userExamService.deleteExam(examId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

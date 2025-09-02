package com.pullit.cbt.controller;

import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.cbt.dto.response.CbtExamLifecycleResponse;
import com.pullit.cbt.service.CbtExamLifecycleService;
import com.pullit.common.annotation.AuthUser;
import com.pullit.common.dto.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/cbt/lifecycle")
@RequiredArgsConstructor
@Tag(name = "CBT Lifecycle", description = "CBT 시험 생명주기 관리 API")
public class CbtLifecycleController {
    
    private final CbtExamLifecycleService cbtExamLifecycleService;
    
    @PostMapping("/start/{examId}")
    @Operation(summary = "CBT 시험 시작", description = "실시간 CBT 시험을 시작하고 학생들에게 알림을 발송합니다")
    public ResponseEntity<ApiResponse<CbtExamLifecycleResponse>> startExam(
            @PathVariable Long examId,
            @RequestParam Long classId,
            @AuthUser CustomUserDetails userDetails
    ) {
        log.info("Starting CBT exam: examId={}, classId={}, teacherId={}", 
                examId, classId, userDetails.getUserId());
        
        CbtExamLifecycleResponse response = cbtExamLifecycleService.startCbtExam(
                examId, classId, userDetails.getUserId());
        
        return ResponseEntity.ok(ApiResponse.success(response, "시험이 시작되었습니다"));
    }
    
    @PostMapping("/end/{examId}")
    @Operation(summary = "CBT 시험 종료", description = "실시간 CBT 시험을 종료하고 학생들에게 알림을 발송합니다")
    public ResponseEntity<ApiResponse<CbtExamLifecycleResponse>> endExam(
            @PathVariable Long examId,
            @RequestParam Long classId,
            @AuthUser CustomUserDetails userDetails
    ) {
        log.info("Ending CBT exam: examId={}, classId={}, teacherId={}", 
                examId, classId, userDetails.getUserId());
        
        CbtExamLifecycleResponse response = cbtExamLifecycleService.endCbtExam(
                examId, classId, userDetails.getUserId());
        
        return ResponseEntity.ok(ApiResponse.success(response, "시험이 종료되었습니다"));
    }
}
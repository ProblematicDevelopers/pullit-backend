package com.pullit.cbt.controller;

import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.cbt.dto.request.CbtExamCreateRequest;
import com.pullit.cbt.service.CbtService;
import com.pullit.common.annotation.AuthUser;
import com.pullit.common.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cbt")
@RequiredArgsConstructor
public class CbtController {
    private final CbtService cbtService;

    @PostMapping("/create")
    @Operation(summary = "cbt 시험지 생성 및 메타 데이터 연결", description = "cbt 시험지 생성 및 메타 데이터 연결")
    public ResponseEntity<ApiResponse<String>> createCbtExam(
            @AuthUser CustomUserDetails currentUser,
            @RequestBody CbtExamCreateRequest request) {
        Long userId = currentUser.getUserId();
        Long examId = cbtService.createExam(userId, request);
        cbtService.addExamItem(examId, request);

        return ResponseEntity.ok(ApiResponse.success("CBT 시험 생성 요청 성공"));
    }

}

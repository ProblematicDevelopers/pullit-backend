package com.pullit.cbt.controller;

import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.cbt.dto.response.AttemptExamResponse;
import com.pullit.cbt.entity.AttemptExam;
import com.pullit.cbt.service.ReportServiceImpl;
import com.pullit.common.annotation.AuthUser;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.item.enums.AreaCode;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportServiceImpl reportService;

    @GetMapping("/attempt/{areaCode}")
    @Operation(summary = "exam_attempt 리스트 조회", description = "areaCode 조건 기반 exam_attempt 리스트 조회")
    public ResponseEntity<ApiResponse<List<AttemptExamResponse>>> getReport(
                @PathVariable AreaCode areaCode,
                @AuthUser CustomUserDetails currentUser
            ) {
        List<AttemptExamResponse> attemptExamList
                = reportService.findAttemptExamBySubjectId(currentUser.getUserId(), areaCode);

        return ResponseEntity.ok(
            ApiResponse.success(attemptExamList, "Exam Attempt List 조회 성공")
        );
    }
}

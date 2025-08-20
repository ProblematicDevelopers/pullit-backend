package com.pullit.cbt.controller;

import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.cbt.dto.response.AttemptExamResponse;
import com.pullit.cbt.entity.AttemptExam;
import com.pullit.cbt.service.ReportService;
import com.pullit.common.annotation.AuthUser;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.item.dto.response.ItemSearchResponse;
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

    private final ReportService reportService;

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

    @GetMapping("/attempt/{attemptId}/basic")
    @Operation(summary = "시험 응시 상세 정보 조회", description = "attempt_id에 대한 시험지 정보, 정답, 사용자 답변 조회")
    public ResponseEntity<ApiResponse<AttemptExamResponse>> getAttemptExamDetail(
                @PathVariable Long attemptId,
                @AuthUser CustomUserDetails currentUser
            ) {
        AttemptExamResponse detailResponse
                = reportService.findAttemptExamDetailById(attemptId, currentUser.getUserId());

        return ResponseEntity.ok(
            ApiResponse.success(detailResponse, "시험 응시 상세 정보 조회 성공")
        );
    }

    @GetMapping("/questions/{questionId}")
    @Operation(summary = "question_id로 문항 정보 조회", description = "CBT 시험의 question_id를 사용하여 해당 문항의 상세 정보를 조회합니다 (item_metadata + item_html_data)")
    public ResponseEntity<ApiResponse<ItemSearchResponse>> getQuestionItem(
            @PathVariable Long questionId) {
        ItemSearchResponse item = reportService.getItemByQuestionId(questionId);
        return ResponseEntity.ok(ApiResponse.success(item, "문항 정보 조회 성공"));
    }
}

package com.pullit.cbt.controller;

import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.cbt.dto.response.AttemptExamResponse;
import com.pullit.cbt.dto.response.DetailDifficultyResponse;
import com.pullit.cbt.dto.response.DetailErrataResponse;
import com.pullit.cbt.dto.response.DetailEvaluationResponse;
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

    // 과목 코드 기반 시험 응시 리스트 조회
    @GetMapping("/attempt/{areaCode}")
    @Operation(summary = "exam_attempt 리스트 조회", description = "areaCode 조건 기반 exam_attempt 리스트 조회")
    public ResponseEntity<ApiResponse<List<AttemptExamResponse>>> getReport(
                @PathVariable AreaCode areaCode,
                @AuthUser CustomUserDetails currentUser
            ) {
        return ResponseEntity.ok(
            ApiResponse.success(
                reportService.findAttemptExamBySubjectId(currentUser.getUserId(), areaCode),
                "Exam Attempt List 조회 성공"
            )
        );
    }

    // 시험 id 기반 상세 정오표 조회
    @GetMapping("/detailerrata/{examId}")
    @Operation(summary = "detail errata 조회", description = "시험 id 기반 상세 정오표 조회")
    public ResponseEntity<ApiResponse<List<DetailErrataResponse>>> getDetailErrata(
            @PathVariable Long examId,
            @AuthUser CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        reportService.findDetailErrataByExamId(examId, currentUser.getUserId()),
                        "Detail Errata 조회 성공"
                )
        );
    }

    // 난이도별 성취율 및 평균 정답률 조회
    @GetMapping("/detaildifficulty/{examId}")
    @Operation(summary = "detail diffculty 조회", description = "난이도별 성취율 + 평균 정답율 조회")
    public ResponseEntity<ApiResponse<List<DetailDifficultyResponse>>> getDetailDifficulty(
            @PathVariable Long examId,
            @AuthUser CustomUserDetails currentUser
    ){
        return ResponseEntity.ok(
                ApiResponse.success(
                        reportService.findDetailDifficultyByExamId(currentUser.getUserId(), examId),
                        "Detail Difficulty 조회 성공"
                )
        );
    }

    // 평가영역별 성취율 및 평균 정답률 조회
    @GetMapping("/detailevaluation/{examId}")
    @Operation(summary = "detail evaluation 조회", description = "평가영역별 성취율 + 평균 정답률 조회")
    public ResponseEntity<ApiResponse<List<DetailEvaluationResponse>>> getDetailEvaluation(
            @PathVariable Long examId,
            @AuthUser CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        reportService.findDetailEvaluationByExamId(currentUser.getUserId(), examId),
                        "Detail Evaluation 조회 성공"
                )
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

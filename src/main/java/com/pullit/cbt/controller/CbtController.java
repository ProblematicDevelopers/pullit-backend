package com.pullit.cbt.controller;

import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.cbt.dto.request.CbtExamCreateRequest;
import com.pullit.cbt.dto.request.CbtAttemptRequest;
import com.pullit.cbt.dto.request.RedisUpdateRequest;
import com.pullit.cbt.dto.request.RedisMigrationRequest;
import com.pullit.cbt.dto.response.CbtExamResponse;
import com.pullit.cbt.dto.response.CbtAttemptResponse;
import com.pullit.cbt.dto.response.AttemptAnswerResponse;
import com.pullit.cbt.dto.response.RedisUpdateResponse;
import com.pullit.cbt.dto.response.RedisDataResponse;
import com.pullit.cbt.dto.response.RedisMigrationResponse;
import com.pullit.cbt.service.CbtService;
import com.pullit.common.annotation.AuthUser;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.exam.entity.UserExam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cbt")
@RequiredArgsConstructor
@Tag(name = "Cbt", description = "CBT API")
public class CbtController {
    private final CbtService cbtService;

    @PostMapping("/create")
    @Operation(summary = "cbt 시험지 생성 및 메타 데이터 연결", description = "cbt 시험지 생성 및 메타 데이터 연결")
    public ResponseEntity<ApiResponse<CbtExamResponse>> createCbtExam(
            @AuthUser CustomUserDetails currentUser,
            @RequestBody CbtExamCreateRequest request) {
        try {
            Long userId = currentUser.getUserId();
            Long examId = cbtService.createExam(userId, request);
            UserExam exam = cbtService.addExamItem(examId, request);
            CbtExamResponse response = CbtExamResponse.builder()
                    .examId(exam.getId())
                    .examName(exam.getExamName())
                    .examType(exam.getExamType())
                    .build();

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            // 잘못된 요청에 대한 예외 처리
            return ResponseEntity.badRequest().body(ApiResponse.error("401", "잘못된 요청입니다: " + e.getMessage()));
        } catch (Exception e) {
            // 기타 예외 처리
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("500", "CBT 시험지 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/attempt")
    @Operation(summary = "CBT 시험 시도 생성 또는 기존 시도 조회", description = "CBT 시험 시도 생성 또는 기존 시도 조회")
    public ResponseEntity<ApiResponse<CbtAttemptResponse>> createOrGetAttempt(
            @AuthUser CustomUserDetails currentUser,
            @RequestBody CbtAttemptRequest request) {
        try {
            Long userId = currentUser.getUserId();
            CbtAttemptResponse response = cbtService.createOrGetAttempt(userId, request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            // 잘못된 요청에 대한 예외 처리
            return ResponseEntity.badRequest().body(ApiResponse.error("401", "잘못된 요청입니다: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            // 기타 예외 처리
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("500", "CBT 시험 시도 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/exam/{examId}")
    @Operation(summary = "생성된 Cbt Exam 정보 불러오기", description = "생성된 Cbt Exam 정보 불러오기")
    public ResponseEntity<ApiResponse<CbtExamResponse>> getCbtExam(
            @AuthUser CustomUserDetails currentUser,
            @PathVariable Long examId) {
        try {
            Long userId = currentUser.getUserId();
            CbtExamResponse response = cbtService.getCbtExam(examId, userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            // 잘못된 요청에 대한 예외 처리
            return ResponseEntity.badRequest().body(ApiResponse.error("401", "잘못된 요청입니다: " + e.getMessage()));
        } catch (Exception e) {
            // 기타 예외 처리
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("500", "CBT 시험지 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/attempt/{attemptId}/answers")
    @Operation(summary = "CBT 시험 시도 답안 조회", description = "CBT 시험 시도 답안 조회")
    public ResponseEntity<ApiResponse<AttemptAnswerResponse>> getAttemptAnswers(
            @AuthUser CustomUserDetails currentUser,
            @PathVariable Long attemptId) {
        try {
            Long userId = currentUser.getUserId();
            AttemptAnswerResponse response = cbtService.getAttemptAnswers(attemptId, userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            // 잘못된 요청에 대한 예외 처리
            return ResponseEntity.badRequest().body(ApiResponse.error("401", "잘못된 요청입니다: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            // 기타 예외 처리
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("500", "CBT 시험 시도 답안 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/redis/{attemptId}")
    @Operation(summary = "CBT 시험 시도 Redis 데이터 업데이트", description = "CBT 시험 시도 Redis 데이터 업데이트")
    public ResponseEntity<ApiResponse<RedisUpdateResponse>> updateRedisData(
            @AuthUser CustomUserDetails currentUser,
            @PathVariable Long attemptId,
            @RequestBody RedisUpdateRequest request) {
        try {
            Long userId = currentUser.getUserId();
            RedisUpdateResponse response = cbtService.updateRedisData(attemptId, request, userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            // 잘못된 요청에 대한 예외 처리
            return ResponseEntity.badRequest().body(ApiResponse.error("401", "잘못된 요청입니다: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            // 기타 예외 처리
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("500", "Redis 데이터 업데이트 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/redis/{attemptId}")
    @Operation(summary = "CBT 시험 시도 Redis 데이터 조회", description = "CBT 시험 시도 Redis 데이터 조회")
    public ResponseEntity<ApiResponse<RedisDataResponse>> getRedisData(
            @AuthUser CustomUserDetails currentUser,
            @PathVariable Long attemptId) {
        try {
            Long userId = currentUser.getUserId();
            RedisDataResponse response = cbtService.getRedisData(attemptId, userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            // 잘못된 요청에 대한 예외 처리
            return ResponseEntity.badRequest().body(ApiResponse.error("401", "잘못된 요청입니다: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            // 기타 예외 처리
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("500", "Redis 데이터 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/attempt/{attemptId}/migrate")
    @Operation(summary = "Redis 데이터를 DB로 마이그레이션", description = "CBT 시험 시도 Redis 데이터를 DB로 마이그레이션")
    public ResponseEntity<ApiResponse<RedisMigrationResponse>> migrateRedisToDatabase(
            @AuthUser CustomUserDetails currentUser,
            @PathVariable Long attemptId,
            @RequestBody RedisMigrationRequest request) {
        try {
            Long userId = currentUser.getUserId();
            RedisMigrationResponse response = cbtService.migrateRedisToDatabase(attemptId, request, userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            // 잘못된 요청에 대한 예외 처리
            return ResponseEntity.badRequest().body(ApiResponse.error("401", "잘못된 요청입니다: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            // 기타 예외 처리
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("500", "Redis 데이터 마이그레이션 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

}

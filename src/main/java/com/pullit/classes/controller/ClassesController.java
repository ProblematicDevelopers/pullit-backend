package com.pullit.classes.controller;

import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.cbt.dto.request.RedisMigrationRequest;
import com.pullit.cbt.dto.request.RedisUpdateRequest;
import com.pullit.cbt.dto.response.AttemptAnswerResponse;
import com.pullit.cbt.dto.response.CbtExamResponse;
import com.pullit.cbt.dto.response.RedisDataResponse;
import com.pullit.cbt.dto.response.RedisMigrationResponse;
import com.pullit.cbt.dto.response.RedisUpdateResponse;
import com.pullit.classes.dto.request.LiveExamAttemptRequest;
import com.pullit.classes.dto.response.ClassDetailResponse;
import com.pullit.classes.dto.response.LiveExamAttemptResponse;
import com.pullit.classes.service.ClassesService;
import com.pullit.common.annotation.AuthUser;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.exam.dto.response.UserExamSchoolResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
@Tag(name = "Classes", description = "클래스 API")
public class ClassesController {

    private final ClassesService classesService;

    /**
     * 유저 ID로 클래스 상세 정보 조회 (교사 정보 + 학생 목록 포함)
     */
    @GetMapping("/myclass")
    @Operation(summary = "유저 ID로 클래스 상세 정보 조회 (교사 정보 + 학생 목록 포함)", description = "유저 ID로 클래스 상세 정보 조회 (교사 정보 + 학생 목록 포함)")
    public ResponseEntity<ApiResponse<ClassDetailResponse>> getClassDetailsByUserId(
            @AuthUser CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        // List<ClassDetailResponse> classDetails =
        // classesService.getClassDetailsByUserId(userId);
        ClassDetailResponse classDetail = classesService.getClassDetailById(userId);
        return ResponseEntity.ok(ApiResponse.success(classDetail));
    }

    @GetMapping("/{classId}/exams")
    @Operation(summary = "클래스 ID로 클래스의 공개 시험 목록 조회", description = "클래스 ID로 클래스의 공개 시험 목록 조회")
    public ResponseEntity<ApiResponse<List<UserExamSchoolResponse>>> getExamsByClassId(
            @PathVariable Long classId) {
        List<UserExamSchoolResponse> exams = classesService.getExamsByClassId(classId);
        return ResponseEntity.ok(ApiResponse.success(exams));
    }

    @GetMapping("/{classId}/exams/{examId}")
    @Operation(summary = "클래스 ID로 클래스의 공개 시험 목록 조회", description = "클래스 ID로 클래스의 공개 시험 목록 조회")
    public ResponseEntity<ApiResponse<UserExamSchoolResponse>> getExamsByClassIdAndExamId(
            @PathVariable Long classId,
            @PathVariable Long examId) {
        UserExamSchoolResponse exam = classesService.getExamsByClassIdAndExamId(classId, examId);
        return ResponseEntity.ok(ApiResponse.success(exam));
    }

    @PostMapping("/attempt")
    @Operation(summary = "실시간 시험 시도 생성 또는 기존 시도 조회", description = "실시간 시험 시도 생성 또는 기존 시도 조회")
    public ResponseEntity<ApiResponse<LiveExamAttemptResponse>> createOrGetAttempt(
            @AuthUser CustomUserDetails currentUser,
            @RequestBody LiveExamAttemptRequest request) {
        try {
            Long userId = currentUser.getUserId();
            LiveExamAttemptResponse response = classesService.createOrGetAttempt(userId, request);
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
    public ResponseEntity<ApiResponse<CbtExamResponse>> getLiveExam(
            @AuthUser CustomUserDetails currentUser,
            @PathVariable Long examId) {
        try {
            Long userId = currentUser.getUserId();
            CbtExamResponse response = classesService.getLiveExam(examId, userId);
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
            AttemptAnswerResponse response = classesService.getAttemptAnswers(attemptId, userId);
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
            RedisUpdateResponse response = classesService.updateRedisData(attemptId, request, userId);
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
            RedisDataResponse response = classesService.getRedisData(attemptId, userId);
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
            RedisMigrationResponse response = classesService.migrateRedisToDatabase(attemptId, request, userId);
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

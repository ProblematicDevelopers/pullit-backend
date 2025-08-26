package com.pullit.classes.controller;

import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.classes.dto.response.ClassDetailResponse;
import com.pullit.classes.service.ClassesService;
import com.pullit.common.annotation.AuthUser;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.exam.dto.response.UserExamSchoolResponse;
import com.pullit.exam.enums.ExamVisibility;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
        @AuthUser CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        // List<ClassDetailResponse> classDetails = classesService.getClassDetailsByUserId(userId);
        ClassDetailResponse classDetail = classesService.getClassDetailById(userId);
        return ResponseEntity.ok(ApiResponse.success(classDetail));
    }

    @GetMapping("/{classId}/exams")
    @Operation(summary = "클래스 ID로 클래스의 공개 시험 목록 조회", description = "클래스 ID로 클래스의 공개 시험 목록 조회")
    public ResponseEntity<ApiResponse<List<UserExamSchoolResponse>>> getExamsByClassId(
        @PathVariable Long classId
    ) {
        List<UserExamSchoolResponse> exams = classesService.getExamsByClassId(classId);
        return ResponseEntity.ok(ApiResponse.success(exams));
    }

    @GetMapping("/{classId}/exams/{examId}")
    @Operation(summary = "클래스 ID로 클래스의 공개 시험 목록 조회", description = "클래스 ID로 클래스의 공개 시험 목록 조회")
    public ResponseEntity<ApiResponse<UserExamSchoolResponse>> getExamsByClassIdAndExamId(
        @PathVariable Long classId,
        @PathVariable Long examId
    ) {
        UserExamSchoolResponse exam = classesService.getExamsByClassIdAndExamId(classId, examId);
        return ResponseEntity.ok(ApiResponse.success(exam));
    }
}

package com.pullit.teacher.controller;

import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.common.annotation.AuthUser;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.teacher.dto.response.*;
import com.pullit.teacher.service.TeacherStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teacher/stats")
@Tag(name = "Teacher Stats", description = "교사용 성적 관리 API")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherStatsController {
    
    private final TeacherStatsService teacherStatsService;
    
    // 클래스 전체 성적 개요
    @GetMapping("/class/{classId}/overview")
    @Operation(summary = "클래스 성적 개요", description = "클래스 전체 학생들의 성적 통계 개요")
    public ResponseEntity<ApiResponse<ClassGradeOverviewResponse>> getClassGradeOverview(
            @PathVariable Long classId,
            @AuthUser CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        teacherStatsService.getClassGradeOverview(classId, currentUser.getUserId()),
                        "클래스 성적 개요 조회 성공"
                )
        );
    }
    
    // 클래스 전체 학생 성적 목록
    @GetMapping("/class/{classId}/students")
    @Operation(summary = "전체 학생 성적 조회", description = "클래스의 모든 학생 성적 상세 조회")
    public ResponseEntity<ApiResponse<List<StudentGradeResponse>>> getAllStudentsGrades(
            @PathVariable Long classId,
            @RequestParam(required = false) Long examId,
            @AuthUser CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        teacherStatsService.getAllStudentsGrades(classId, examId, currentUser.getUserId()),
                        "학생 성적 목록 조회 성공"
                )
        );
    }
    
    // 특정 시험의 전체 결과
    @GetMapping("/class/{classId}/exam/{examId}")
    @Operation(summary = "시험별 상세 결과", description = "특정 시험의 전체 학생 결과 및 통계")
    public ResponseEntity<ApiResponse<ExamResultDetailResponse>> getExamResultDetail(
            @PathVariable Long classId,
            @PathVariable Long examId,
            @AuthUser CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        teacherStatsService.getExamResultDetail(classId, examId, currentUser.getUserId()),
                        "시험 결과 상세 조회 성공"
                )
        );
    }
    
    // 개별 학생 성적 상세
    @GetMapping("/class/{classId}/student/{studentId}")
    @Operation(summary = "개별 학생 성적 상세", description = "특정 학생의 모든 시험 성적 조회")
    public ResponseEntity<ApiResponse<StudentDetailGradeResponse>> getStudentGradeDetail(
            @PathVariable Long classId,
            @PathVariable Long studentId,
            @AuthUser CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        teacherStatsService.getStudentGradeDetail(classId, studentId, currentUser.getUserId()),
                        "학생 성적 상세 조회 성공"
                )
        );
    }
    
    // 클래스 성적 분포도
    @GetMapping("/class/{classId}/distribution")
    @Operation(summary = "성적 분포도", description = "클래스 전체 성적 분포 통계")
    public ResponseEntity<ApiResponse<GradeDistributionResponse>> getGradeDistribution(
            @PathVariable Long classId,
            @RequestParam(required = false) Long examId,
            @AuthUser CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        teacherStatsService.getGradeDistribution(classId, examId, currentUser.getUserId()),
                        "성적 분포도 조회 성공"
                )
        );
    }
    
    // 시험별 비교 통계
    @GetMapping("/class/{classId}/comparison")
    @Operation(summary = "시험별 비교", description = "여러 시험 간 성적 비교 분석")
    public ResponseEntity<ApiResponse<List<ExamComparisonResponse>>> getExamComparison(
            @PathVariable Long classId,
            @AuthUser CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        teacherStatsService.getExamComparison(classId, currentUser.getUserId()),
                        "시험 비교 분석 조회 성공"
                )
        );
    }
}
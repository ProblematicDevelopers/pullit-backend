package com.pullit.exam.controller;

import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.common.annotation.AuthUser;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.exam.dto.request.TeacherLiveExamRequest;
import com.pullit.exam.dto.response.TeacherLiveExamResponse;
import com.pullit.exam.service.TeacherLiveExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/teacher-live-exams")
@RequiredArgsConstructor
@Tag(name = "Teacher Live Exams", description = "선생님 실시간 시험 관리 API")
public class TeacherLiveExamController {
    
    private final TeacherLiveExamService teacherLiveExamService;
    
    /**
     * 실시간 시험 생성
     */
    @PostMapping
    @Operation(summary = "실시간 시험 생성", description = "선생님이 실시간 시험을 생성합니다")
    public ResponseEntity<ApiResponse<TeacherLiveExamResponse>> createLiveExam(
            @AuthUser CustomUserDetails userDetails,
            @Valid @RequestBody TeacherLiveExamRequest request) {
        
        log.info("Creating live exam: teacher={}, class={}", userDetails.getUserId(), request.getClassId());
        
        TeacherLiveExamResponse response = teacherLiveExamService.createLiveExam(
                userDetails.getUserId(), request);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 클래스의 실시간 시험 목록 조회
     */
    @GetMapping("/class/{classId}")
    @Operation(summary = "클래스 실시간 시험 목록", description = "특정 클래스의 모든 실시간 시험을 조회합니다")
    public ResponseEntity<ApiResponse<List<TeacherLiveExamResponse>>> getClassLiveExams(
            @PathVariable Long classId) {
        
        List<TeacherLiveExamResponse> exams = teacherLiveExamService.getClassLiveExams(classId);
        return ResponseEntity.ok(ApiResponse.success(exams));
    }
    
    /**
     * 클래스의 활성 시험 목록 조회
     */
    @GetMapping("/class/{classId}/active")
    @Operation(summary = "활성 실시간 시험 목록", description = "클래스의 활성 상태 실시간 시험을 조회합니다")
    public ResponseEntity<ApiResponse<List<TeacherLiveExamResponse>>> getActiveExams(
            @PathVariable Long classId) {
        
        List<TeacherLiveExamResponse> exams = teacherLiveExamService.getActiveExams(classId);
        return ResponseEntity.ok(ApiResponse.success(exams));
    }
    
    /**
     * 현재 진행 중인 시험 조회
     */
    @GetMapping("/class/{classId}/current")
    @Operation(summary = "현재 진행 중인 시험", description = "클래스의 현재 진행 중인 시험을 조회합니다")
    public ResponseEntity<ApiResponse<TeacherLiveExamResponse>> getCurrentExam(
            @PathVariable Long classId) {
        
        TeacherLiveExamResponse exam = teacherLiveExamService.getCurrentExam(classId);
        return ResponseEntity.ok(ApiResponse.success(exam));
    }
    
    /**
     * 오늘 예정된 시험 목록
     */
    @GetMapping("/class/{classId}/today")
    @Operation(summary = "오늘 예정된 시험", description = "오늘 예정된 실시간 시험 목록을 조회합니다")
    public ResponseEntity<ApiResponse<List<TeacherLiveExamResponse>>> getTodaysExams(
            @PathVariable Long classId) {
        
        List<TeacherLiveExamResponse> exams = teacherLiveExamService.getTodaysExams(classId);
        return ResponseEntity.ok(ApiResponse.success(exams));
    }
    
    /**
     * 시험 시작
     */
    @PostMapping("/{examId}/start")
    @Operation(summary = "시험 시작", description = "실시간 시험을 시작합니다")
    public ResponseEntity<ApiResponse<TeacherLiveExamResponse>> startExam(
            @AuthUser CustomUserDetails userDetails,
            @PathVariable Long examId) {
        
        log.info("Starting exam: examId={}, teacher={}", examId, userDetails.getUserId());
        
        TeacherLiveExamResponse response = teacherLiveExamService.startExam(
                examId, userDetails.getUserId());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 시험 종료
     */
    @PostMapping("/{examId}/end")
    @Operation(summary = "시험 종료", description = "실시간 시험을 종료합니다")
    public ResponseEntity<ApiResponse<TeacherLiveExamResponse>> endExam(
            @AuthUser CustomUserDetails userDetails,
            @PathVariable Long examId) {
        
        log.info("Ending exam: examId={}, teacher={}", examId, userDetails.getUserId());
        
        TeacherLiveExamResponse response = teacherLiveExamService.endExam(
                examId, userDetails.getUserId());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * WebSocket 메시지 핸들러 - 시험 상태 업데이트
     */
    @MessageMapping("/exam/status")
    @SendTo("/topic/exam/status")
    public TeacherLiveExamResponse handleExamStatusUpdate(@Payload TeacherLiveExamResponse examStatus) {
        log.info("Broadcasting exam status update: examId={}, status={}", 
                examStatus.getId(), examStatus.getExamStatus());
        return examStatus;
    }
}
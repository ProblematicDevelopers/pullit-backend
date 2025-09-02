package com.pullit.domain.assignment.controller;

import com.pullit.domain.assignment.dto.request.SubmissionGradeRequest;
import com.pullit.domain.assignment.dto.response.SubmissionDetailResponse;
import com.pullit.domain.assignment.dto.response.SubmissionListResponse;
import com.pullit.domain.assignment.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
@Tag(name = "Submission", description = "과제 제출 관리 API")
public class SubmissionController {
    
    private final SubmissionService submissionService;
    
    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "과제 제출", description = "학생이 과제를 제출합니다")
    public ResponseEntity<SubmissionDetailResponse> submitAssignment(
            @RequestParam Long assignmentId,
            @RequestParam Long studentId,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(required = false) String comment) {
        
        log.info("과제 제출 요청: assignmentId={}, studentId={}", assignmentId, studentId);
        SubmissionDetailResponse response = submissionService.submitAssignment(
                assignmentId, studentId, files, comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/assignment/{assignmentId}/student/{studentId}")
    @Operation(summary = "학생 제출물 조회", description = "특정 학생의 제출물을 조회합니다")
    public ResponseEntity<SubmissionDetailResponse> getStudentSubmission(
            @PathVariable Long assignmentId,
            @PathVariable Long studentId) {
        
        log.info("학생 제출물 조회: assignmentId={}, studentId={}", assignmentId, studentId);
        SubmissionDetailResponse response = submissionService.getStudentSubmission(assignmentId, studentId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/assignment/{assignmentId}")
    @Operation(summary = "과제별 제출 현황 조회", description = "특정 과제의 모든 제출물을 조회합니다 (선생님용)")
    public ResponseEntity<Page<SubmissionListResponse>> getAssignmentSubmissions(
            @PathVariable Long assignmentId,
            @RequestHeader("X-Teacher-Id") Long teacherId,
            @PageableDefault(size = 20, sort = "submittedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        log.info("과제별 제출 현황 조회: assignmentId={}, teacherId={}", assignmentId, teacherId);
        Page<SubmissionListResponse> response = submissionService.getAssignmentSubmissions(
                assignmentId, teacherId, pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/assignment/{assignmentId}/stats")
    @Operation(summary = "제출 통계 조회", description = "과제의 제출 현황 통계를 조회합니다")
    public ResponseEntity<Map<String, Long>> getSubmissionStats(
            @PathVariable Long assignmentId,
            @RequestHeader("X-Teacher-Id") Long teacherId) {
        
        log.info("제출 통계 조회: assignmentId={}, teacherId={}", assignmentId, teacherId);
        Map<String, Long> stats = submissionService.getSubmissionStats(assignmentId, teacherId);
        return ResponseEntity.ok(stats);
    }
    
    @PutMapping("/{submissionId}/grade")
    @Operation(summary = "제출물 평가", description = "제출된 과제를 평가합니다")
    public ResponseEntity<SubmissionDetailResponse> gradeSubmission(
            @PathVariable Long submissionId,
            @Valid @RequestBody SubmissionGradeRequest request,
            @RequestHeader("X-Teacher-Id") Long teacherId) {
        
        log.info("제출물 평가: submissionId={}, teacherId={}, score={}", 
                submissionId, teacherId, request.getScore());
        SubmissionDetailResponse response = submissionService.gradeSubmission(
                submissionId, request, teacherId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{submissionId}/files/{fileId}/download")
    @Operation(summary = "제출 파일 다운로드", description = "제출된 파일의 다운로드 URL을 생성합니다")
    public ResponseEntity<String> getSubmissionFileDownloadUrl(
            @PathVariable Long submissionId,
            @PathVariable Long fileId,
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("제출 파일 다운로드 URL 생성: submissionId={}, fileId={}, userId={}", 
                submissionId, fileId, userId);
        String downloadUrl = submissionService.getSubmissionFileDownloadUrl(submissionId, fileId, userId);
        return ResponseEntity.ok(downloadUrl);
    }
    
    @GetMapping("/assignment/{assignmentId}/not-submitted")
    @Operation(summary = "미제출 학생 목록 조회", description = "과제를 제출하지 않은 학생 목록을 조회합니다")
    public ResponseEntity<List<SubmissionListResponse>> getNotSubmittedStudents(
            @PathVariable Long assignmentId,
            @RequestHeader("X-Teacher-Id") Long teacherId) {
        
        log.info("미제출 학생 목록 조회: assignmentId={}, teacherId={}", assignmentId, teacherId);
        List<SubmissionListResponse> response = submissionService.getNotSubmittedStudents(
                assignmentId, teacherId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/teacher/{teacherId}/pending")
    @Operation(summary = "평가 대기 제출물 조회", description = "평가 대기중인 제출물 목록을 조회합니다")
    public ResponseEntity<List<SubmissionListResponse>> getPendingGradingSubmissions(
            @PathVariable Long teacherId) {
        
        log.info("평가 대기 제출물 조회: teacherId={}", teacherId);
        List<SubmissionListResponse> response = submissionService.getPendingGradingSubmissions(teacherId);
        return ResponseEntity.ok(response);
    }
}
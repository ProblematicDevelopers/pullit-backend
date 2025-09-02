package com.pullit.domain.assignment.controller;

import com.pullit.domain.assignment.dto.request.AssignmentCreateRequest;
import com.pullit.domain.assignment.dto.request.AssignmentUpdateRequest;
import com.pullit.domain.assignment.dto.response.AssignmentDetailResponse;
import com.pullit.domain.assignment.dto.response.AssignmentListResponse;
import com.pullit.domain.assignment.service.AssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@Slf4j
@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
@Tag(name = "Assignment", description = "과제 관리 API")
public class AssignmentController {
    
    private final AssignmentService assignmentService;
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "과제 생성", description = "새로운 과제를 생성합니다")
    public ResponseEntity<AssignmentDetailResponse> createAssignment(
            @Valid @RequestPart("assignment") AssignmentCreateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        
        log.info("과제 생성 요청: title={}, teacherId={}", request.getTitle(), request.getTeacherId());
        AssignmentDetailResponse response = assignmentService.createAssignment(request, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "과제 수정", description = "과제 정보를 수정합니다")
    public ResponseEntity<AssignmentDetailResponse> updateAssignment(
            @PathVariable Long id,
            @Valid @RequestBody AssignmentUpdateRequest request,
            @RequestHeader("X-Teacher-Id") Long teacherId) {
        
        log.info("과제 수정 요청: id={}, teacherId={}", id, teacherId);
        AssignmentDetailResponse response = assignmentService.updateAssignment(id, request, teacherId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/publish")
    @Operation(summary = "과제 발행", description = "과제를 발행하여 학생들에게 공개합니다")
    public ResponseEntity<Void> publishAssignment(
            @PathVariable Long id,
            @RequestHeader("X-Teacher-Id") Long teacherId) {
        
        log.info("과제 발행 요청: id={}, teacherId={}", id, teacherId);
        assignmentService.publishAssignment(id, teacherId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "과제 삭제", description = "과제를 삭제합니다 (초안 상태만 가능)")
    public ResponseEntity<Void> deleteAssignment(
            @PathVariable Long id,
            @RequestHeader("X-Teacher-Id") Long teacherId) {
        
        log.info("과제 삭제 요청: id={}, teacherId={}", id, teacherId);
        assignmentService.deleteAssignment(id, teacherId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/teacher/{teacherId}")
    @Operation(summary = "선생님 과제 목록 조회", description = "선생님이 생성한 과제 목록을 조회합니다")
    public ResponseEntity<Page<AssignmentListResponse>> getTeacherAssignments(
            @PathVariable Long teacherId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        log.info("선생님 과제 목록 조회: teacherId={}", teacherId);
        Page<AssignmentListResponse> response = assignmentService.getTeacherAssignments(teacherId, pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/student/{studentId}")
    @Operation(summary = "학생 과제 목록 조회", description = "학생에게 할당된 과제 목록을 조회합니다")
    public ResponseEntity<List<AssignmentListResponse>> getStudentAssignments(
            @PathVariable Long studentId,
            @RequestParam List<Long> classIds) {
        
        log.info("학생 과제 목록 조회: studentId={}, classIds={}", studentId, classIds);
        List<AssignmentListResponse> response = assignmentService.getStudentAssignments(studentId, classIds);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "과제 상세 조회", description = "과제 상세 정보를 조회합니다")
    public ResponseEntity<AssignmentDetailResponse> getAssignmentDetail(@PathVariable Long id) {
        log.info("과제 상세 조회: id={}", id);
        AssignmentDetailResponse response = assignmentService.getAssignmentDetail(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{assignmentId}/files/{fileId}/download")
    @Operation(summary = "과제 파일 다운로드", description = "과제 첨부 파일의 다운로드 URL을 생성합니다")
    public ResponseEntity<String> getAssignmentFileDownloadUrl(
            @PathVariable Long assignmentId,
            @PathVariable Long fileId) {
        
        log.info("과제 파일 다운로드 URL 생성: assignmentId={}, fileId={}", assignmentId, fileId);
        String downloadUrl = assignmentService.getAssignmentFileDownloadUrl(assignmentId, fileId);
        return ResponseEntity.ok(downloadUrl);
    }
}
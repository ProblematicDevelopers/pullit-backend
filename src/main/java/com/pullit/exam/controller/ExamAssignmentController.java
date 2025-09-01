package com.pullit.exam.controller;

import com.pullit.common.dto.response.ApiResponse;
import com.pullit.exam.dto.request.ExamAssignmentRequest;
import com.pullit.exam.dto.response.ExamAssignmentResponse;
import com.pullit.exam.service.ExamAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 시험 출제 관련 API 컨트롤러
 * 시험을 여러 학급에 배정하고 관리하는 기능을 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
@Tag(name = "Exam Assignment", description = "시험 출제 관리 API")
public class ExamAssignmentController {

    private final ExamAssignmentService examAssignmentService;

    /**
     * 시험을 여러 학급에 출제합니다.
     * @NotificationTrigger 어노테이션을 통해 학생들에게 자동으로 알림이 발송됩니다.
     * 
     * @param request 시험 출제 요청 정보 (examId, classIds[], examDate, examTime, timeLimit, sendNotification 등)
     * @return 출제된 시험 정보와 알림 발송 결과
     */
    @PostMapping("/assign")
    @Operation(
        summary = "시험 출제",
        description = "시험을 여러 학급에 동시에 출제하고 학생들에게 알림을 발송합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "시험 출제 성공"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (날짜/시간 오류, 유효성 검증 실패)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "시험 또는 학급을 찾을 수 없음"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "이미 출제된 시험 (중복 출제)"
        )
    })
    // @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")  // 선생님 또는 관리자만 출제 가능
    public ResponseEntity<ApiResponse<ExamAssignmentResponse>> assignExamToClasses(
            @Valid @RequestBody ExamAssignmentRequest request) {
        
        log.info("시험 출제 API 호출 - examId: {}, classCount: {}, sendNotification: {}", 
                request.getExamId(), 
                request.getClassIds().size(),
                request.getSendNotification());
        
        ExamAssignmentResponse response = examAssignmentService.assignExamToClasses(request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                    response,
                    "시험이 성공적으로 출제되었습니다."
                ));
    }

    /**
     * 특정 학급의 예정된 시험 목록을 조회합니다.
     * 
     * @param classId 학급 ID
     * @return 예정된 시험 목록
     */
    @GetMapping("/assignments/class/{classId}")
    @Operation(
        summary = "학급별 예정된 시험 조회",
        description = "특정 학급에 배정된 예정 시험 목록을 조회합니다."
    )
    // @PreAuthorize("hasRole('TEACHER') or hasRole('STUDENT') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ExamAssignmentResponse>>> getUpcomingExamsByClass(
            @PathVariable @Parameter(description = "학급 ID") Long classId) {
        
        log.info("학급별 예정 시험 조회 - classId: {}", classId);
        
        List<ExamAssignmentResponse> assignments = examAssignmentService.getUpcomingExamsByClassId(classId);
        
        return ResponseEntity.ok(
            ApiResponse.success(
                assignments,
                String.format("%d개의 예정된 시험이 있습니다.", assignments.size())
            )
        );
    }

    /**
     * 특정 시험의 출제 정보를 조회합니다.
     * 
     * @param examId 시험 ID
     * @return 시험 출제 정보
     */
    @GetMapping("/assignments/exam/{examId}")
    @Operation(
        summary = "시험별 출제 정보 조회",
        description = "특정 시험이 출제된 모든 학급 정보를 조회합니다."
    )
    // @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ExamAssignmentResponse>>> getAssignmentsByExam(
            @PathVariable @Parameter(description = "시험 ID") Long examId) {
        
        log.info("시험별 출제 정보 조회 - examId: {}", examId);
        
        List<ExamAssignmentResponse> assignments = examAssignmentService.getAssignmentsByExamId(examId);
        
        return ResponseEntity.ok(
            ApiResponse.success(
                assignments,
                assignments.isEmpty() ? "출제된 시험이 없습니다." : "출제 정보를 조회했습니다."
            )
        );
    }

    /**
     * 시험 출제를 취소합니다.
     * 
     * @param assignmentId 출제 ID
     * @return 취소 결과
     */
    @DeleteMapping("/assignments/{assignmentId}")
    @Operation(
        summary = "시험 출제 취소",
        description = "예정된 시험 출제를 취소합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "출제 취소 성공"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "출제 정보를 찾을 수 없음"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "취소할 수 없는 상태 (이미 진행 중이거나 완료된 시험)"
        )
    })
    // @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cancelAssignment(
            @PathVariable @Parameter(description = "출제 ID") Long assignmentId) {
        
        log.info("시험 출제 취소 요청 - assignmentId: {}", assignmentId);
        
        examAssignmentService.cancelAssignment(assignmentId);
        
        return ResponseEntity.ok(
            ApiResponse.success(null, "시험 출제가 취소되었습니다.")
        );
    }
}
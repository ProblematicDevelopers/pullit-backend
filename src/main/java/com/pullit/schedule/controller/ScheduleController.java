package com.pullit.schedule.controller;

import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.common.annotation.AuthUser;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.schedule.dto.request.ScheduleCreateRequest;
import com.pullit.schedule.dto.request.ScheduleUpdateRequest;
import com.pullit.schedule.dto.response.ScheduleResponse;
import com.pullit.schedule.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Schedule", description = "일정 관리 API")
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
@Slf4j
public class ScheduleController {
    
    private final ScheduleService scheduleService;
    
    @Operation(summary = "일정 생성", description = "새로운 일정을 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleResponse>> createSchedule(
            @AuthUser CustomUserDetails userDetails,
            @Valid @RequestBody ScheduleCreateRequest request) {
        log.info("일정 생성 요청: userId={}, title={}", userDetails.getUserId(), request.getTitle());
        
        ScheduleResponse response = scheduleService.createSchedule(userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }
    
    @Operation(summary = "일정 조회", description = "일정 ID로 일정을 조회합니다.")
    @GetMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduleResponse>> getSchedule(
            @AuthUser CustomUserDetails userDetails,
            @PathVariable Long scheduleId) {
        log.info("일정 조회 요청: userId={}, scheduleId={}", userDetails.getUserId(), scheduleId);
        
        ScheduleResponse response = scheduleService.getSchedule(scheduleId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @Operation(summary = "일정 수정", description = "일정 정보를 수정합니다.")
    @PutMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduleResponse>> updateSchedule(
            @AuthUser CustomUserDetails userDetails,
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleUpdateRequest request) {
        log.info("일정 수정 요청: userId={}, scheduleId={}", userDetails.getUserId(), scheduleId);
        
        ScheduleResponse response = scheduleService.updateSchedule(scheduleId, userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @Operation(summary = "일정 삭제", description = "일정을 삭제합니다.")
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(
            @AuthUser CustomUserDetails userDetails,
            @PathVariable Long scheduleId) {
        log.info("일정 삭제 요청: userId={}, scheduleId={}", userDetails.getUserId(), scheduleId);
        
        scheduleService.deleteSchedule(scheduleId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    @Operation(summary = "월별 일정 조회", description = "특정 월의 일정을 조회합니다.")
    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getMonthlySchedules(
            @AuthUser CustomUserDetails userDetails,
            @RequestParam int year,
            @RequestParam int month) {
        log.info("월별 일정 조회 요청: userId={}, year={}, month={}", userDetails.getUserId(), year, month);
        
        List<ScheduleResponse> schedules = scheduleService.getMonthlySchedules(userDetails.getUserId(), year, month);
        return ResponseEntity.ok(ApiResponse.success(schedules));
    }
    
    @Operation(summary = "날짜 범위 일정 조회", description = "특정 날짜 범위의 일정을 조회합니다.")
    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getSchedulesByDateRange(
            @AuthUser CustomUserDetails userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("날짜 범위 일정 조회 요청: userId={}, startDate={}, endDate={}", userDetails.getUserId(), startDate, endDate);
        
        List<ScheduleResponse> schedules = scheduleService.getSchedulesByDateRange(userDetails.getUserId(), startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(schedules));
    }
    
    @Operation(summary = "일정 상태 변경", description = "일정의 상태를 변경합니다.")
    @PatchMapping("/{scheduleId}/status")
    public ResponseEntity<ApiResponse<ScheduleResponse>> updateScheduleStatus(
            @AuthUser CustomUserDetails userDetails,
            @PathVariable Long scheduleId,
            @RequestParam String status) {
        log.info("일정 상태 변경 요청: userId={}, scheduleId={}, status={}", userDetails.getUserId(), scheduleId, status);
        
        ScheduleResponse response = scheduleService.updateScheduleStatus(scheduleId, userDetails.getUserId(), status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
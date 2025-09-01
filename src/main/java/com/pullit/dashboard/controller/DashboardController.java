package com.pullit.dashboard.controller;

import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.common.annotation.AuthUser;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.dashboard.dto.response.DashboardActivityResponse;
import com.pullit.dashboard.dto.response.DashboardScheduleResponse;
import com.pullit.dashboard.dto.response.DashboardStatsResponse;
import com.pullit.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Dashboard", description = "대시보드 API")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {
    
    private final DashboardService dashboardService;
    
    @Operation(summary = "최근 활동 조회", description = "선생님의 최근 활동 내역을 조회합니다.")
    @GetMapping("/activities")
    public ResponseEntity<ApiResponse<List<DashboardActivityResponse>>> getRecentActivities(
            @AuthUser CustomUserDetails userDetails,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("최근 활동 조회 요청: userId={}, limit={}", userDetails.getUserId(), limit);
        
        List<DashboardActivityResponse> activities = dashboardService.getRecentActivities(userDetails.getUserId(), limit);
        return ResponseEntity.ok(ApiResponse.success(activities));
    }
    
    @Operation(summary = "예정된 일정 조회", description = "예정된 시험 및 일정을 조회합니다.")
    @GetMapping("/schedules")
    public ResponseEntity<ApiResponse<List<DashboardScheduleResponse>>> getUpcomingSchedules(
            @AuthUser CustomUserDetails userDetails,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("예정된 일정 조회 요청: userId={}, limit={}", userDetails.getUserId(), limit);
        
        List<DashboardScheduleResponse> schedules = dashboardService.getUpcomingSchedules(userDetails.getUserId(), limit);
        return ResponseEntity.ok(ApiResponse.success(schedules));
    }
    
    @Operation(summary = "대시보드 통계 조회", description = "대시보드 통계 정보를 조회합니다.")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats(
            @AuthUser CustomUserDetails userDetails) {
        log.info("대시보드 통계 조회 요청: userId={}", userDetails.getUserId());
        
        DashboardStatsResponse stats = dashboardService.getDashboardStats(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
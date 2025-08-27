package com.pullit.classes.controller;

import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.classes.dto.response.StatsDetailResponse;
import com.pullit.classes.dto.response.StatsLineResponse;
import com.pullit.classes.repository.StatsRepository;
import com.pullit.classes.service.StatsService;
import com.pullit.classes.service.StatsServiceImpl;
import com.pullit.common.annotation.AuthUser;
import com.pullit.common.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/classes")
@Tag(name = "stats", description = "클래스 통계 API")
public class StatsController {
    private final StatsRepository statsRepository;
    private final StatsService statsService;

    // 클래스 시험별 내 점수 / 전체 평균 점수 조회
    @GetMapping("/statsline/{classId}")
    @Operation(summary = "클래스 시험별 평점", description = "클래스 내 시험별 내 점수 및 평균 점수 조회 기능")
    public ResponseEntity<ApiResponse<List<StatsLineResponse>>> getStatsLines(
            @PathVariable Long classId,
            @AuthUser CustomUserDetails currentUser
            ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        statsService.findStatsLines(currentUser.getUserId(), classId),
                        "stats line 조회 성공"
                )
        );
    }

    // 클래스 시험별 세부 통계 데이터 조회
    @GetMapping("/statsdetail/{classId}")
    @Operation(summary = "클래스 시험별 세부 통계", description = "클래스 내 시험별 석차, 백분위, 사분위수")
    public ResponseEntity<ApiResponse<List<StatsDetailResponse>>> getStatsDetail(
            @PathVariable Long classId,
            @AuthUser CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        statsService.findStatsDetail(currentUser.getUserId(), classId),
                        "stats detail 조회 성공"
                )
        );
    }


}

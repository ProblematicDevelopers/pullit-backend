package com.pullit.teacher.controller;

import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.common.annotation.AuthUser;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.teacher.dto.response.TeacherResponse;
import com.pullit.teacher.dto.request.TeacherUpdateRequest;
import com.pullit.teacher.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
@Tag(name = "Teacher", description = "선생님 관련 API")
public class TeacherController {
    
    private final TeacherService teacherService;

    @GetMapping("/{userId}")
    @Operation(summary = "선생님 정보 조회", description = "사용자 ID로 선생님 정보를 조회합니다")
    public ResponseEntity<ApiResponse<TeacherResponse>> getTeacherByUserId(@PathVariable Long userId) {
        TeacherResponse teacherResponse = teacherService.getTeacherByUserId(userId);
        return ResponseEntity.ok().body(ApiResponse.success(teacherResponse));
    }

    @GetMapping("/me")
    @Operation(summary = "내 선생님 정보 조회", description = "현재 로그인한 선생님의 정보를 조회합니다")
    public ResponseEntity<ApiResponse<TeacherResponse>> getMyTeacherInfo(@AuthUser CustomUserDetails userDetails) {
        TeacherResponse teacherResponse = teacherService.getTeacherByUserId(userDetails.getUserId());
        return ResponseEntity.ok().body(ApiResponse.success(teacherResponse));
    }

    @PutMapping("/me")
    @Operation(summary = "내 선생님 정보 수정", description = "현재 로그인한 선생님의 정보를 수정합니다")
    public ResponseEntity<ApiResponse<TeacherResponse>> updateMyTeacherInfo(
            @AuthUser CustomUserDetails userDetails,
            @RequestBody TeacherUpdateRequest request) {
        TeacherResponse teacherResponse = teacherService.updateTeacher(userDetails.getUserId(), request);
        return ResponseEntity.ok().body(ApiResponse.success(teacherResponse));
    }
}

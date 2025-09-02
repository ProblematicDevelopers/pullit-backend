package com.pullit.student.controller;

import com.pullit.common.dto.response.ApiResponse;
import com.pullit.student.dto.request.StudentUpdateRequest;
import com.pullit.student.dto.response.StudentResponse;
import com.pullit.student.service.StudentService;
import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.common.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {
    private final StudentService studentService;

    @GetMapping("/{userId}")
    @Operation(summary = "학생 학년 조회", description = "학생 학년")
    public ResponseEntity<ApiResponse<StudentResponse>> findByUserId(@PathVariable Long userId) {
        StudentResponse studentResponse = studentService.findByUserId(userId);
        return ResponseEntity.ok().body(ApiResponse.success(studentResponse));
    }

    @PutMapping("/me")
    @Operation(summary = "내 학생 정보 수정", description = "현재 로그인한 학생의 정보를 수정합니다")
    public ResponseEntity<ApiResponse<StudentResponse>> updateMyStudentInfo(
            @AuthUser CustomUserDetails userDetails,
            @RequestBody StudentUpdateRequest request) {
        StudentResponse studentResponse = studentService.updateStudent(userDetails.getUserId(), request);
        return ResponseEntity.ok().body(ApiResponse.success(studentResponse));
    }
}

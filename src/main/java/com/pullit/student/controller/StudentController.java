package com.pullit.student.controller;

import com.pullit.common.dto.response.ApiResponse;
import com.pullit.student.dto.response.StudentResponse;
import com.pullit.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}

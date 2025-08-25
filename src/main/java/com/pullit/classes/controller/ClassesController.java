package com.pullit.classes.controller;

import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.classes.dto.response.ClassDetailResponse;
import com.pullit.classes.service.ClassesService;
import com.pullit.common.annotation.AuthUser;
import com.pullit.common.dto.response.ApiResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
@Tag(name = "Classes", description = "클래스 API")
public class ClassesController {

    private final ClassesService classesService;
    /**
     * 유저 ID로 클래스 상세 정보 조회 (교사 정보 + 학생 목록 포함)
     */
    @GetMapping("/myclass")
    public ResponseEntity<ApiResponse<ClassDetailResponse>> getClassDetailsByUserId(
        @AuthUser CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        // List<ClassDetailResponse> classDetails = classesService.getClassDetailsByUserId(userId);
        ClassDetailResponse classDetail = classesService.getClassDetailById(userId);
        return ResponseEntity.ok(ApiResponse.success(classDetail));
    }
   
}

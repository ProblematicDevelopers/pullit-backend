package com.pullit.item.controller;


import com.pullit.common.dto.response.ApiResponse;
import com.pullit.item.dto.response.SubjectResponse;
import com.pullit.item.service.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subject")
@RequiredArgsConstructor
public class SubjectController {
    private final SubjectService subjectService;

    @GetMapping
    @Operation(summary = "교과서 리스트 전체 조회", description = "교과서 전체 리스트")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> findAll() {
        List<SubjectResponse> res = subjectService.findAllSubjectsOnly();
        return ResponseEntity.ok(ApiResponse.success(res));
    }
}

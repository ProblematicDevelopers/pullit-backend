package com.pullit.item.controller;


import com.pullit.common.dto.response.ApiResponse;
import com.pullit.item.dto.response.SubjectResponse;
import com.pullit.item.service.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subject")
@RequiredArgsConstructor
public class SubjectController {
    private final SubjectService subjectService;

    @GetMapping
    @Operation(summary = "교과서 리스트 전체 조회", description = "교과서 전체 리스트")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> findAll(
            @RequestParam(required = false) Boolean includeTextbooks,
            @RequestParam(required = false) List<String> grades
    ) {
        // includeTextbooks와 grades 파라미터는 현재 무시하고 전체 목록 반환
        // TODO: 필요시 필터링 로직 추가
        List<SubjectResponse> res = subjectService.findAllSubjectsOnly();
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    @GetMapping("/filter")
    @Operation(summary = "학년과 과목으로 교과서 필터링",
            description = "학년 코드와 과목 코드로 해당하는 교과서 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> findAllSubjectsOnly(
            @RequestParam(required = false) String gradeCode,
            @RequestParam(required = false) String areaCode
    ) {
        List<SubjectResponse> res = subjectService.findByGradeCodeAndAreaCode(gradeCode, areaCode);
        return ResponseEntity.ok(ApiResponse.success(res));
    }
}

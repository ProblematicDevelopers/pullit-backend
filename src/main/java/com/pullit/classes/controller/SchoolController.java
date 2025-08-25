package com.pullit.classes.controller;

import com.pullit.classes.entity.School;
import com.pullit.classes.service.SchoolService;
import com.pullit.exam.service.ExamSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/schools")
@RequiredArgsConstructor
@Tag(name = "School", description = "학교 검색 API")
public class SchoolController {

    private final SchoolService schoolService;

    /**
     * 학교명으로 학교 검색
     * @param keyword 검색 키워드
     * @return 검색된 학교 목록
     */
    @GetMapping("/search")
    @Operation(summary = "학교 검색", description = "학교명으로 학교를 검색합니다")
    public ResponseEntity<List<School>> searchSchools(@RequestParam String keyword) {
        log.info("학교 검색 요청: keyword={}", keyword);

        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<School> schools = schoolService.findBySchoolNameContaining(keyword.trim());
        log.info("학교 검색 완료: {} 건", schools.size());

        return ResponseEntity.ok(schools);
    }

    /**
     * 모든 학교 조회 (테스트용)
     */
    @GetMapping
    @Operation(summary = "전체 학교 조회", description = "등록된 모든 학교를 조회합니다")
    public ResponseEntity<List<School>> getAllSchools() {
        log.info("전체 학교 조회 요청");
        List<School> schools = schoolService.findAll();
        return ResponseEntity.ok(schools);
    }

    private final ExamSearchService examSearchService;


}

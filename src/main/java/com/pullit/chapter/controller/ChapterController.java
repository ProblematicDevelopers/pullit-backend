package com.pullit.chapter.controller;

import com.pullit.chapter.dto.response.ChapterResponse;
import com.pullit.chapter.dto.response.LargeNode;
import com.pullit.chapter.service.ChapterService;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.item.entity.Subject;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chapter")
@RequiredArgsConstructor
public class ChapterController {
    private final ChapterService chapterService;

    @GetMapping
    @Operation(summary="챕터 리스트 전체", description = "챕터 리스트 전체")
    public ResponseEntity<ApiResponse<List<ChapterResponse>>> findAllChapterOnly(){
        List<ChapterResponse> chapters = chapterService.findAllChapterOnly();
        return ResponseEntity.ok(ApiResponse.success(chapters));
    }

    @GetMapping("/{subjectId}")
    @Operation(summary="챕터 리스트 전체", description = "챕터 리스트 전체")
    public ResponseEntity<ApiResponse<List<ChapterResponse>>> findBySubjectId(@PathVariable Long subjectId){
        List<ChapterResponse> chapters = chapterService.findBySubjectId(subjectId);

        return ResponseEntity.ok(ApiResponse.success(chapters));
    }

    @GetMapping("/{subjectId}/tree")
    @Operation(summary = "과목별 챕터 트리 조회")
    public ResponseEntity<ApiResponse<List<LargeNode>>> getChapterTree(@PathVariable Long subjectId) {
        var tree = chapterService.findTreeBySubjectId(subjectId);
        return ResponseEntity.ok(ApiResponse.success(tree));
    }
}
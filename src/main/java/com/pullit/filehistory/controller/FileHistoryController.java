package com.pullit.filehistory.controller;

import com.pullit.common.constants.ServiceConstants;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.filehistory.dto.response.ProblemSourceOptionResponse;
import com.pullit.filehistory.service.FileHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/file-history")
@RequiredArgsConstructor
@Tag(name = "File History", description = "문제 가공 파일 관련 API")
public class FileHistoryController {
    private final FileHistoryService fileHistoryService;

    //TODO: 교과서 OR CBT 선택하는 페이지
    @GetMapping("/source")
    @Operation(summary = "교과서 OR CBT 모드 선택", description = "문제 가공 모드 선택 페이지. 교과서 혹은 CBT 둘중 하나를 선택합니다. ")
    public ResponseEntity<ApiResponse<List<ProblemSourceOptionResponse>>> selectProblemSource() {
        List<ProblemSourceOptionResponse> problemSourceOptionResponses = List.of(
                new ProblemSourceOptionResponse(
                        ServiceConstants.CBT_CODE,
                        ServiceConstants.CBT_NAME,
                        ServiceConstants.CBT_DESC),
                new ProblemSourceOptionResponse(
                        ServiceConstants.TEXTBOOK_CODE,
                        ServiceConstants.TEXTBOOK_NAME,
                        ServiceConstants.TEXTBOOK_DESC)
        );
        return ResponseEntity.ok(ApiResponse.success(problemSourceOptionResponses));
    }
    //TODO: 교과서 선택시 교과서 리스트 반환
    //TODO: 교과서 내 문제
    //TODO: CBT 선택시 CBT 리스트 반환
    //TODO: CBT 새로운 문제지 선택
    //TODO: CBT 기존 문제지 선택
    //TODO: (교과서 OR CBT) 문제 추가
}

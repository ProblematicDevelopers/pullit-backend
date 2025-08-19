package com.pullit.filehistory.controller;

import com.pullit.common.constants.ServiceConstants;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.exam.entity.Exam;
import com.pullit.filehistory.dto.response.ProblemSourceOptionResponse;
import com.pullit.filehistory.service.FileHistoryService;
import com.pullit.item.dto.response.SubjectResponse;
import com.pullit.item.service.SubjectService;
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
    private final SubjectService subjectService;

    //TODO: 교과서 선택시 교과서 리스트 반환
    @GetMapping("/textbook")
    @Operation(summary = "교과서 선택", description = "교과서 리스트를 출력합니다")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> findAllTextbook() {
        List<SubjectResponse> subjects = subjectService.findAllSubjectsOnly();
        return ResponseEntity.ok(ApiResponse.success(subjects));
    }

    //TODO: pdf 업로드시 file history 저장

}

// 참고: 교과서 선택으로 바로 빠지기때문에 해당 함수는 주석 처리만 해둠
//    @GetMapping("/source")
//    @Operation(summary = "교과서 OR CBT 모드 선택", description = "문제 가공 모드 선택 페이지. 교과서 혹은 CBT 둘중 하나를 선택합니다. ")
//    public ResponseEntity<ApiResponse<List<ProblemSourceOptionResponse>>> selectProblemSource() {
//        List<ProblemSourceOptionResponse> problemSourceOptionResponses = List.of(
//                new ProblemSourceOptionResponse(
//                        ServiceConstants.CBT_CODE,
//                        ServiceConstants.CBT_NAME,
//                        ServiceConstants.CBT_DESC),
//                new ProblemSourceOptionResponse(
//                        ServiceConstants.TEXTBOOK_CODE,
//                        ServiceConstants.TEXTBOOK_NAME,
//                        ServiceConstants.TEXTBOOK_DESC)
//        );
//        return ResponseEntity.ok(ApiResponse.success(problemSourceOptionResponses));
//    }
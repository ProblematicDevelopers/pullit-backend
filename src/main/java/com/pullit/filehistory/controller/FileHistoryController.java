package com.pullit.filehistory.controller;

import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.common.annotation.AuthUser;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.filehistory.service.FileHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/file-history")
@RequiredArgsConstructor
@Tag(name = "File History", description = "문제 가공 파일 히스토리 관련 API")
public class FileHistoryController {
    private final FileHistoryService fileHistoryService;
    @Operation(summary = "파일 업로드 후 내역 저장", description = "file s3 저장 후 호출 api. id를 받아 파일 히스토리 저장")
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Long>> createFileHistory(@RequestParam Long fileMetadataId, @RequestParam Long subjectId, @AuthUser CustomUserDetails currentUser) {
        Long fileHistoryId = fileHistoryService.createHistory(fileMetadataId, subjectId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(fileHistoryId, "파일 히스토리 생성 완료"));
    }

}
//    //TODO: 교과서 선택시 교과서 리스트 반환
//    @GetMapping("/textbook")
//    @Operation(summary = "교과서 선택", description = "교과서 리스트를 출력합니다")
//    public ResponseEntity<ApiResponse<List<SubjectResponse>>> findAllTextbook() {
//        List<SubjectResponse> subjects = subjectService.findAllSubjectsOnly();
//        return ResponseEntity.ok(ApiResponse.success(subjects));
//    }
//
//    //TODO: pdf 업로드시 file history 저장
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
//package com.pullit.exam.service;
//
//import com.pullit.common.s3.dto.S3UploadResponse;
//import com.pullit.common.s3.service.S3Service;
//import com.pullit.exam.dto.request.TestWizardExamCreateRequest;
//import com.pullit.exam.dto.response.UserExamResponse;
//import com.pullit.exam.entity.UserExam;
//import com.pullit.exam.entity.UserExamItem;
//import com.pullit.exam.enums.ExamVisibility;
//import com.pullit.exam.repository.UserExamRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.coyote.BadRequestException;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.time.LocalDate;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//@Transactional(readOnly = true)
//public class TestWizardExamService {
//    private final UserExamRepository userExamRepository;
//    private final S3Service s3Service;
//
//    @Transactional
//    public UserExamResponse createTestWizardExam(
//            TestWizardExamCreateRequest request,
//            MultipartFile pdfFile){
//
//        S3UploadResponse s3Response = null;
//        if(pdfFile!=null && !pdfFile.isEmpty()){
//            s3Response = uploadPdfToS3(pdfFile, request.getExamName());
//            log.info("PDF 업로드 완료:{}", s3Response.getS3Key());
//        }
//        UserExam exam=UserExam.builder()
//                .examName(request.getExamName())
//                .gradeCode(request.getGradeCode())
//                .gradeName(request.getGradeName())
//                .termCode(request.getTermCode())
//                .termName(request.getTermName())
//                .areaCode(request.getAreaCode())
//                .areaName(request.getAreaName())
//                .examType("TESTWIZARD")
//                .visibility(request.getVisibility())
//                .classId(request.getClassId())
//                .timeLimit(request.getTimeLimit())
//                .examDate(request.getExamDate())
//                .description(request.getDescription())
//                .build();
//
//        if(s3Response!=null){
//            exam.updatePdfMetadata(s3Response);
//        }
//
//        request.getItems().forEach(itemRequest ->{
//            UserExamItem item = UserExamItem.builder()
//                    .userExam(exam)
//                    .itemId(itemRequest.getItemId())
//                    .subjectId(itemRequest.getSubjectId())
//                    .itemOrder(itemRequest.getItemOrder())
//                    .points(itemRequest.getPoints())
//                    .build();
//            exam.addExamItem(item);
//        });
//
//        UserExam savedExam =  userExamRepository.save(exam);
//
//        log.info("TestWizard 시험지 생성 완료: ID={}, Visibility={}, ClassId={}",
//                savedExam.getId(), savedExam.getVisibility(), savedExam.getClassId());
//
//        return UserExamResponse.from(savedExam);
//
//    }
//
////    private void validRequest(TestWizardExamCreateRequest request){
////        // CLASS_ONLY인데 classId가 없는 경우
////        if (request.getVisibility() == ExamVisibility.CLASS_ONLY
////                && request.getClassId() == null) {
////            throw new BadRequestException("클래스 전용 시험은 클래스를 선택해야 합니다.");
////        }
////
////        // 시험 날짜가 과거인 경우
////        if (request.getExamDate() != null
////                && request.getExamDate().isBefore(LocalDate.now())) {
////            throw new BadRequestException("시험 날짜는 오늘 이후여야 합니다.");
////        }
////
////        // 제한 시간 검증
////        if (request.getTimeLimit() != null
////                && (request.getTimeLimit() < 10 || request.getTimeLimit() > 300)) {
////            throw new BadRequestException("제한 시간은 10분 이상 120분 이하여야 합니다.");
////        }
////    }
//
////    private S3UploadResponse uploadpdfToS3(MultipartFile pdfFile, String examName){
////        try{
////            String fileName = String.format()
////        }
////    }
//
//}

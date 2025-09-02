package com.pullit.exam.service;

import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;
import com.pullit.common.s3.dto.S3UploadResponse;
import com.pullit.common.s3.enums.S3Directory;
import com.pullit.common.s3.service.S3Service;
import com.pullit.common.file.service.FileService;
import com.pullit.common.file.dto.request.FileUploadRequest;
import com.pullit.common.file.dto.response.FileUploadResponse;
import com.pullit.common.file.enums.FileCategory;
import com.pullit.common.file.repository.FileMetadataRepository;
import com.pullit.common.file.entity.FileMetadata;
import com.pullit.exam.dto.request.UserExamCreateRequest;
import com.pullit.exam.dto.response.UserExamResponse;
import com.pullit.exam.entity.UserExam;
import com.pullit.exam.entity.UserExamItem;
import com.pullit.exam.enums.ExamVisibility;
import com.pullit.exam.repository.UserExamRepository;
import com.pullit.item.dao.ItemMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserExamServiceImpl implements UserExamService {

    private final UserExamRepository userExamRepository;
    private final ItemMetadataRepository itemMetadataRepository;
    private final S3Service s3Service;
    private final FileService fileService;
    private final FileMetadataRepository fileMetadataRepository;

    @Override
    public UserExamResponse createExam(UserExamCreateRequest request, String pdfUrl, String answerPdfUrl) {
        log.info("시험지 생성 시작: examName={}", request.getExamName());
        
        // UserExam 엔티티 생성
        UserExam userExam = UserExam.builder()
                .examName(request.getExamName())
                .gradeCode(request.getGradeCode())
                .gradeName(request.getGradeName())
                .termCode(request.getTermCode())
                .termName(request.getTermName())
                .areaCode(request.getAreaCode())
                .areaName(request.getAreaName())
                .examType(request.getExamType())
                .totalPoints(request.getTotalPoints() != null ? request.getTotalPoints() : 100)
                .timeLimit(request.getTimeLimit())
                .examDate(request.getExamDate())
                .description(request.getDescription())
                .visibility(ExamVisibility.valueOf(request.getVisibility()))
                .classId(request.getClassId())
                .build();

        // 문항 추가 (점수는 총점/문항수로 균등 분배)
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            List<UserExamItem> examItems = new ArrayList<>();
            int itemCount = request.getItems().size();
            int totalPoints = userExam.getTotalPoints() != null ? userExam.getTotalPoints() : 100;
            int base = itemCount > 0 ? totalPoints / itemCount : 0;
            int remainder = itemCount > 0 ? totalPoints % itemCount : 0;
            int idx = 0;
            for (UserExamCreateRequest.ExamItemRequest itemRequest : request.getItems()) {
                // 실제 ItemMetadata 엔티티 확인 (존재 여부 체크)
                if (itemRequest.getItemId() != null) {
                    itemMetadataRepository.findByItemId(itemRequest.getItemId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
                }
                int points = base + (idx < remainder ? 1 : 0);
                UserExamItem examItem = UserExamItem.builder()
                        .itemId(itemRequest.getItemId())
                        .subjectId(itemRequest.getSubjectId())
                        .itemOrder(itemRequest.getItemOrder())
                        .points(points)
                        .build();
                userExam.addExamItem(examItem);
                examItems.add(examItem);
                idx++;
            }
        }

        // PDF URL 설정
        if (pdfUrl != null) {
            userExam.updatePdfUrl(pdfUrl);
        }
        if (answerPdfUrl != null) {
            userExam.updateAnswerPdfUrl(answerPdfUrl);
        }

        // 저장
        UserExam savedExam = userExamRepository.save(userExam);
        log.info("시험지 생성 완료: examId={}, totalItems={}", savedExam.getId(), savedExam.getTotalItems());

        return convertToResponse(savedExam);
    }

    /**
     * MultipartFile로 PDF를 받아서 S3에 업로드하고 시험지 생성
     */
    @Transactional
    public UserExamResponse createExamWithPDF(UserExamCreateRequest request, MultipartFile pdfFile) {
        log.info("PDF와 함께 시험지 생성: examName={}, pdfSize={}", 
                request.getExamName(), pdfFile != null ? pdfFile.getSize() : 0);
        
        String pdfUrl = null;
        S3UploadResponse s3Response = null;
        FileUploadResponse fileResponse = null;
        Long fileMetadataId = null;
        
        // PDF 파일이 있으면 S3에 업로드 및 FileMetadata 저장
        if (pdfFile != null && !pdfFile.isEmpty()) {
            try {
                // FileService를 통해 파일 업로드 (S3 + FileMetadata DB 저장)
                FileUploadRequest fileRequest = FileUploadRequest.builder()
                        .directory(S3Directory.EXAM_PDF)
                        .category(FileCategory.DOCUMENT)
                        .entityType("UserExam")
                        .description("시험지 PDF: " + request.getExamName())
                        .isPublic(false)
                        .build();
                
                // TODO: 실제 사용자 ID를 가져와야 함 (현재는 임시로 1L 사용)
                Long userId = 1L;
                fileResponse = fileService.uploadFile(pdfFile, fileRequest, userId);
                fileMetadataId = fileResponse.getFileId();
                
                // S3 응답 정보 설정
                s3Response = S3UploadResponse.builder()
                        .s3Key(fileResponse.getS3Key())
                        .fileName(fileResponse.getOriginalFilename())
                        .fileSize(fileResponse.getFileSize())
                        .contentType(fileResponse.getContentType())
                        .presignedUrl(fileResponse.getPresignedUrl())
                        .publicUrl(fileResponse.getPublicUrl())
                        .build();
                
                pdfUrl = fileResponse.getPresignedUrl();
                log.info("PDF 업로드 성공: fileId={}, s3Key={}, url={}", 
                        fileResponse.getFileId(), fileResponse.getS3Key(), pdfUrl);
            } catch (Exception e) {
                log.error("PDF 업로드 실패", e);
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
            }
        }
        
        // 시험지 생성
        UserExam userExam = UserExam.builder()
                .examName(request.getExamName())
                .gradeCode(request.getGradeCode())
                .gradeName(request.getGradeName())
                .termCode(request.getTermCode())
                .termName(request.getTermName())
                .areaCode(request.getAreaCode())
                .areaName(request.getAreaName())
                .examType(request.getExamType() != null ? request.getExamType() : "TESTWIZARD")
                .totalPoints(request.getTotalPoints() != null ? request.getTotalPoints() : 100)
                .timeLimit(request.getTimeLimit())
                .examDate(request.getExamDate())
                .description(request.getDescription())
                .visibility(request.getVisibility() != null ? 
                        ExamVisibility.valueOf(request.getVisibility()) : ExamVisibility.PRIVATE)
                .classId(request.getClassId())
                .build();

        // S3 업로드 결과 저장
        if (s3Response != null) {
            userExam.updatePdfMetadata(s3Response);
        }

        // 문항 추가 (점수는 총점/문항수로 균등 분배)
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            int itemCount = request.getItems().size();
            int totalPoints = userExam.getTotalPoints() != null ? userExam.getTotalPoints() : 100;
            int base = itemCount > 0 ? totalPoints / itemCount : 0;
            int remainder = itemCount > 0 ? totalPoints % itemCount : 0;
            int idx = 0;
            for (UserExamCreateRequest.ExamItemRequest itemRequest : request.getItems()) {
                // ItemMetadata 존재 여부 체크 (선택적)
                if (itemRequest.getItemId() != null) {
                    boolean itemExists = itemMetadataRepository.findByItemId(itemRequest.getItemId()).isPresent();
                    if (!itemExists) {
                        log.warn("ItemMetadata not found but continuing: itemId={}", itemRequest.getItemId());
                    }
                }
                int points = base + (idx < remainder ? 1 : 0);
                UserExamItem examItem = UserExamItem.builder()
                        .itemId(itemRequest.getItemId())
                        .subjectId(itemRequest.getSubjectId())
                        .itemOrder(itemRequest.getItemOrder())
                        .points(points)
                        .build();
                userExam.addExamItem(examItem);
                idx++;
            }
        }

        // 저장
        UserExam savedExam = userExamRepository.save(userExam);
        log.info("시험지 생성 완료: examId={}, totalItems={}, hasPdf={}", 
                savedExam.getId(), savedExam.getTotalItems(), savedExam.hasPdf());

        // FileMetadata의 entityId 업데이트
        if (fileMetadataId != null && savedExam.getId() != null) {
            try {
                updateFileMetadataEntityId(fileMetadataId, savedExam.getId());
                log.info("FileMetadata entityId 업데이트 완료: fileId={}, examId={}", 
                        fileMetadataId, savedExam.getId());
            } catch (Exception e) {
                log.error("FileMetadata entityId 업데이트 실패", e);
                // 실패해도 계속 진행
            }
        }

        return convertToResponse(savedExam);
    }

    @Override
    public UserExamResponse updatePdfUrl(Long examId, String pdfUrl) {
        UserExam exam = userExamRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXAM_NOT_FOUND));
        
        exam.updatePdfUrl(pdfUrl);
        UserExam updatedExam = userExamRepository.save(exam);
        
        return convertToResponse(updatedExam);
    }

    @Override
    @Transactional(readOnly = true)
    public UserExamResponse getExam(Long examId) {
        UserExam exam = userExamRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXAM_NOT_FOUND));
        
        return convertToResponse(exam);
    }

    @Override
    public void deleteExam(Long examId) {
        UserExam exam = userExamRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXAM_NOT_FOUND));
        
        // S3에서 PDF 파일 삭제
        if (exam.getPdfS3Key() != null) {
            try {
                s3Service.delete(exam.getPdfS3Key());
                log.info("S3 PDF 파일 삭제 완료: s3Key={}", exam.getPdfS3Key());
            } catch (Exception e) {
                log.error("S3 PDF 파일 삭제 실패: s3Key={}", exam.getPdfS3Key(), e);
            }
        }
        
        // Soft Delete
        exam.softDelete(1L); // TODO: 실제 사용자 ID로 변경 필요
        userExamRepository.save(exam);
        
        log.info("시험지 삭제 완료: examId={}", examId);
    }

    private UserExamResponse convertToResponse(UserExam exam) {
        return UserExamResponse.builder()
                .id(exam.getId())
                .examName(exam.getExamName())
                .gradeCode(exam.getGradeCode())
                .gradeName(exam.getGradeName())
                .termCode(exam.getTermCode())
                .termName(exam.getTermName())
                .areaCode(exam.getAreaCode())
                .areaName(exam.getAreaName())
                .examType(exam.getExamType())
                .totalItems(exam.getTotalItems())
                .totalPoints(exam.getTotalPoints())
                .timeLimit(exam.getTimeLimit())
                .examDate(exam.getExamDate())
                .description(exam.getDescription())
                .visibility(exam.getVisibility().name())
                .classId(exam.getClassId())
                .pdfUrl(exam.getPdfUrl())
                .answerPdfUrl(exam.getAnswerPdfUrl())
                .pdfGeneratedAt(exam.getPdfGeneratedAt())
                .createdAt(exam.getCreatedDate())
                .updatedAt(exam.getUpdatedDate())
                .build();
    }
    
    /**
     * FileMetadata의 entityId를 업데이트하는 헬퍼 메서드
     */
    private void updateFileMetadataEntityId(Long fileId, Long entityId) {
        fileMetadataRepository.findById(fileId).ifPresent(fileMetadata -> {
            fileMetadata.setEntityId(entityId);
            fileMetadataRepository.save(fileMetadata);
        });
    }
}

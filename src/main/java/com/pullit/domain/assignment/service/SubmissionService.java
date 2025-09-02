package com.pullit.domain.assignment.service;

import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;
import com.pullit.common.s3.dto.S3UploadResponse;
import com.pullit.common.s3.enums.S3Directory;
import com.pullit.common.s3.service.S3Service;
import com.pullit.domain.assignment.dto.request.SubmissionGradeRequest;
import com.pullit.domain.assignment.dto.request.SubmissionUploadRequest;
import com.pullit.domain.assignment.dto.response.SubmissionDetailResponse;
import com.pullit.domain.assignment.dto.response.SubmissionListResponse;
import com.pullit.domain.assignment.entity.Assignment;
import com.pullit.domain.assignment.entity.Submission;
import com.pullit.domain.assignment.entity.SubmissionFile;
import com.pullit.domain.assignment.repository.AssignmentRepository;
import com.pullit.domain.assignment.repository.SubmissionRepository;
import com.pullit.domain.calendar.service.CalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionService {
    
    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final S3Service s3Service;
    private final CalendarService calendarService;
    
    /**
     * 과제 제출
     */
    @Transactional
    public SubmissionDetailResponse submitAssignment(
            Long assignmentId, 
            Long studentId,
            List<MultipartFile> files,
            String comment) {
        
        log.info("과제 제출 시작: assignmentId={}, studentId={}", assignmentId, studentId);
        
        // 과제 확인
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        
        // 과제 상태 확인
        if (assignment.getStatus() != Assignment.AssignmentStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        
        // 마감일 확인
        boolean isLate = LocalDateTime.now().isAfter(assignment.getDueDate());
        if (isLate && !assignment.getAllowLateSubmission()) {
            throw new BusinessException(ErrorCode.SUBMISSION_DEADLINE_PASSED);
        }
        
        // 기존 제출물 조회 또는 생성
        Submission submission = submissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseGet(() -> Submission.builder()
                        .assignment(assignment)
                        .studentId(studentId)
                        .studentName("학생" + studentId) // TODO: 실제 학생 정보 조회
                        .status(Submission.SubmissionStatus.NOT_SUBMITTED)
                        .build());
        
        // 이미 제출한 경우 기존 파일 삭제
        if (submission.getStatus() == Submission.SubmissionStatus.SUBMITTED) {
            deleteExistingFiles(submission);
        }
        
        // 파일 업로드
        if (files != null && !files.isEmpty()) {
            List<SubmissionFile> submissionFiles = uploadSubmissionFiles(files, submission);
            submission.getFiles().clear();
            submission.getFiles().addAll(submissionFiles);
        }
        
        // 제출 처리
        submission.submit();
        
        Submission saved = submissionRepository.save(submission);
        log.info("과제 제출 완료: submissionId={}", saved.getId());
        
        // 캘린더 이벤트 상태 업데이트
        try {
            calendarService.updateAssignmentEventStatus(assignmentId, studentId);
        } catch (Exception e) {
            log.error("캘린더 이벤트 업데이트 실패: assignmentId={}, studentId={}", assignmentId, studentId, e);
            // 캘린더 업데이트 실패는 과제 제출을 막지 않음
        }
        
        return SubmissionDetailResponse.from(saved);
    }
    
    /**
     * 제출 파일 업로드
     */
    private List<SubmissionFile> uploadSubmissionFiles(List<MultipartFile> files, Submission submission) {
        List<SubmissionFile> submissionFiles = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                // S3 업로드
                S3UploadResponse uploadResponse = s3Service.upload(file, S3Directory.SUBMISSION);
                
                // 파일 엔티티 생성
                SubmissionFile submissionFile = SubmissionFile.builder()
                        .submission(submission)
                        .originalFileName(file.getOriginalFilename())
                        .storedFileName(uploadResponse.getS3Key())
                        .filePath(uploadResponse.getS3Key())
                        .fileType(file.getContentType())
                        .fileSize(file.getSize())
                        .build();
                
                submissionFiles.add(submissionFile);
                log.info("제출 파일 업로드 성공: {}", file.getOriginalFilename());
                
            } catch (Exception e) {
                log.error("제출 파일 업로드 실패: {}", file.getOriginalFilename(), e);
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
            }
        }
        
        return submissionFiles;
    }
    
    /**
     * 기존 제출 파일 삭제
     */
    private void deleteExistingFiles(Submission submission) {
        if (submission.getFiles() != null && !submission.getFiles().isEmpty()) {
            List<String> s3Keys = submission.getFiles().stream()
                    .map(SubmissionFile::getStoredFileName)
                    .toList();
            
            s3Service.deleteMultiple(s3Keys);
            log.info("기존 제출 파일 삭제: count={}", s3Keys.size());
        }
    }
    
    /**
     * 학생의 제출물 조회
     */
    public SubmissionDetailResponse getStudentSubmission(Long assignmentId, Long studentId) {
        Submission submission = submissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        
        return SubmissionDetailResponse.from(submission);
    }
    
    /**
     * 과제별 제출 현황 조회 (선생님용)
     */
    public Page<SubmissionListResponse> getAssignmentSubmissions(
            Long assignmentId, 
            Long teacherId,
            Pageable pageable) {
        
        // 과제 확인 및 권한 검증
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        
        if (!assignment.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        
        // 제출물 조회
        Page<Submission> submissions = submissionRepository.findByAssignmentId(assignmentId, pageable);
        
        return submissions.map(SubmissionListResponse::from);
    }
    
    /**
     * 제출 현황 통계 조회
     */
    public Map<String, Long> getSubmissionStats(Long assignmentId, Long teacherId) {
        // 과제 확인 및 권한 검증
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        
        if (!assignment.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        
        // 통계 조회
        List<Object[]> stats = submissionRepository.getSubmissionStatsByAssignmentId(assignmentId);
        
        return stats.stream()
                .collect(Collectors.toMap(
                        stat -> stat[0].toString(),
                        stat -> (Long) stat[1]
                ));
    }
    
    /**
     * 제출물 평가
     */
    @Transactional
    public SubmissionDetailResponse gradeSubmission(
            Long submissionId,
            SubmissionGradeRequest request,
            Long teacherId) {
        
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        
        // 권한 확인
        Assignment assignment = submission.getAssignment();
        if (!assignment.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        
        // 제출 상태 확인
        if (submission.getStatus() != Submission.SubmissionStatus.SUBMITTED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        
        // 점수 유효성 확인
        if (request.getScore() != null && assignment.getMaxScore() != null) {
            if (request.getScore() > assignment.getMaxScore()) {
                throw new BusinessException(ErrorCode.INVALID_SCORE);
            }
        }
        
        // 평가 처리
        submission.grade(request.getScore(), request.getFeedback());
        
        Submission saved = submissionRepository.save(submission);
        log.info("제출물 평가 완료: submissionId={}, score={}", submissionId, request.getScore());
        
        return SubmissionDetailResponse.from(saved);
    }
    
    /**
     * 제출 파일 다운로드 URL 생성
     */
    public String getSubmissionFileDownloadUrl(Long submissionId, Long fileId, Long requesterId) {
        Submission submission = submissionRepository.findByIdWithFiles(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        
        // 권한 확인 (학생 본인 또는 담당 선생님)
        Assignment assignment = submission.getAssignment();
        boolean isStudent = submission.getStudentId().equals(requesterId);
        boolean isTeacher = assignment.getTeacherId().equals(requesterId);
        
        if (!isStudent && !isTeacher) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        
        // 파일 찾기
        SubmissionFile file = submission.getFiles().stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
        
        // S3 Pre-signed URL 생성 (1시간 유효)
        return s3Service.generatePresignedUrl(file.getStoredFileName());
    }
    
    /**
     * 미제출 학생 목록 조회
     */
    public List<SubmissionListResponse> getNotSubmittedStudents(Long assignmentId, Long teacherId) {
        // 과제 확인 및 권한 검증
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        
        if (!assignment.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        
        // 미제출 학생 조회
        List<Submission> notSubmitted = submissionRepository.findNotSubmittedByAssignmentId(assignmentId);
        
        return notSubmitted.stream()
                .map(SubmissionListResponse::from)
                .toList();
    }
    
    /**
     * 평가 대기중인 제출물 조회
     */
    public List<SubmissionListResponse> getPendingGradingSubmissions(Long teacherId) {
        List<Submission> pending = submissionRepository.findPendingGradingByTeacherId(teacherId);
        
        return pending.stream()
                .map(SubmissionListResponse::from)
                .toList();
    }
}
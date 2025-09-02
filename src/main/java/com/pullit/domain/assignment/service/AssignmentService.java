package com.pullit.domain.assignment.service;

import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;
import com.pullit.common.s3.dto.S3UploadResponse;
import com.pullit.common.s3.enums.S3Directory;
import com.pullit.common.s3.service.S3Service;
import com.pullit.domain.assignment.dto.request.AssignmentCreateRequest;
import com.pullit.domain.assignment.dto.request.AssignmentUpdateRequest;
import com.pullit.domain.assignment.dto.response.AssignmentDetailResponse;
import com.pullit.domain.assignment.dto.response.AssignmentListResponse;
import com.pullit.domain.assignment.entity.Assignment;
import com.pullit.domain.assignment.entity.AssignmentClass;
import com.pullit.domain.assignment.entity.AssignmentFile;
import com.pullit.domain.assignment.entity.Submission;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentService {
    
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final S3Service s3Service;
    private final CalendarService calendarService;
    
    /**
     * 과제 생성
     */
    @Transactional
    public AssignmentDetailResponse createAssignment(AssignmentCreateRequest request, List<MultipartFile> files) {
        log.info("과제 생성 시작: title={}, teacherId={}", request.getTitle(), request.getTeacherId());
        
        // 과제 엔티티 생성
        Assignment assignment = Assignment.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .teacherId(request.getTeacherId())
                .dueDate(request.getDueDate())
                .status(Assignment.AssignmentStatus.DRAFT)
                .maxScore(request.getMaxScore())
                .allowLateSubmission(request.getAllowLateSubmission())
                .build();
        
        // 파일 업로드 처리
        if (files != null && !files.isEmpty()) {
            List<AssignmentFile> assignmentFiles = uploadAssignmentFiles(files, assignment, request.getTeacherId());
            assignment.getFiles().addAll(assignmentFiles);
        }
        
        // 반 정보 추가
        List<Long> allStudentIds = new ArrayList<>();
        if (request.getClassIds() != null && !request.getClassIds().isEmpty()) {
            for (Long classId : request.getClassIds()) {
                AssignmentClass assignmentClass = AssignmentClass.builder()
                        .assignment(assignment)
                        .classId(classId)
                        .className(request.getClassNames().get(classId))
                        .build();
                assignment.getAssignmentClasses().add(assignmentClass);
                
                // 해당 반 학생들의 제출물 엔티티 미리 생성
                List<Long> studentIds = createSubmissionsForClass(assignment, classId);
                allStudentIds.addAll(studentIds);
            }
        }
        
        Assignment saved = assignmentRepository.save(assignment);
        log.info("과제 생성 완료: id={}", saved.getId());
        
        // 과제를 캘린더에 추가 (비동기 처리)
        if (!allStudentIds.isEmpty()) {
            calendarService.createAssignmentEvent(saved, allStudentIds);
        }
        
        return AssignmentDetailResponse.from(saved);
    }
    
    /**
     * 과제 파일 업로드
     */
    private List<AssignmentFile> uploadAssignmentFiles(List<MultipartFile> files, Assignment assignment, Long teacherId) {
        List<AssignmentFile> assignmentFiles = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                // S3 업로드
                S3UploadResponse uploadResponse = s3Service.upload(file, S3Directory.ASSIGNMENT);
                
                // 파일 엔티티 생성
                AssignmentFile assignmentFile = AssignmentFile.builder()
                        .assignment(assignment)
                        .originalFileName(file.getOriginalFilename())
                        .storedFileName(uploadResponse.getS3Key())
                        .filePath(uploadResponse.getS3Key())
                        .fileType(file.getContentType())
                        .fileSize(file.getSize())
                        .uploaderId(String.valueOf(teacherId))
                        .build();
                
                assignmentFiles.add(assignmentFile);
                log.info("과제 파일 업로드 성공: {}", file.getOriginalFilename());
                
            } catch (Exception e) {
                log.error("과제 파일 업로드 실패: {}", file.getOriginalFilename(), e);
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
            }
        }
        
        return assignmentFiles;
    }
    
    /**
     * 반별 학생 제출물 엔티티 생성
     */
    private List<Long> createSubmissionsForClass(Assignment assignment, Long classId) {
        // TODO: 실제로는 Class 서비스에서 학생 목록을 가져와야 함
        // 임시로 더미 데이터 생성
        List<Long> studentIds = getStudentsByClassId(classId);
        
        for (Long studentId : studentIds) {
            Submission submission = Submission.builder()
                    .assignment(assignment)
                    .studentId(studentId)
                    .studentName("학생" + studentId) // 실제로는 학생 정보 조회 필요
                    .status(Submission.SubmissionStatus.NOT_SUBMITTED)
                    .isLate(false)
                    .build();
            
            assignment.getSubmissions().add(submission);
        }
        
        return studentIds;
    }
    
    /**
     * 반별 학생 목록 조회 (임시 구현)
     */
    private List<Long> getStudentsByClassId(Long classId) {
        // TODO: 실제 구현 시 Class/Student 서비스 연동
        // 임시로 더미 학생 ID 반환
        return List.of(1L, 2L, 3L, 4L, 5L);
    }
    
    /**
     * 과제 발행
     */
    @Transactional
    public void publishAssignment(Long assignmentId, Long teacherId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        
        // 권한 확인
        if (!assignment.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        
        // 상태 변경
        assignment.publish();
        assignmentRepository.save(assignment);
        
        log.info("과제 발행 완료: id={}", assignmentId);
    }
    
    /**
     * 선생님의 과제 목록 조회
     */
    public Page<AssignmentListResponse> getTeacherAssignments(Long teacherId, Pageable pageable) {
        Page<Assignment> assignments = assignmentRepository.findByTeacherId(teacherId, pageable);
        return assignments.map(AssignmentListResponse::from);
    }
    
    /**
     * 학생의 과제 목록 조회
     */
    public List<AssignmentListResponse> getStudentAssignments(Long studentId, List<Long> classIds) {
        // 학생이 속한 반들의 발행된 과제 조회
        List<Assignment.AssignmentStatus> activeStatuses = List.of(
                Assignment.AssignmentStatus.PUBLISHED,
                Assignment.AssignmentStatus.CLOSED
        );
        
        List<Assignment> assignments = assignmentRepository.findByClassIdsAndStatuses(classIds, activeStatuses);
        
        return assignments.stream()
                .map(assignment -> {
                    // 학생의 제출 상태 확인
                    Submission submission = submissionRepository
                            .findByAssignmentIdAndStudentId(assignment.getId(), studentId)
                            .orElse(null);
                    
                    return AssignmentListResponse.from(assignment, submission);
                })
                .toList();
    }
    
    /**
     * 과제 상세 조회
     */
    public AssignmentDetailResponse getAssignmentDetail(Long assignmentId) {
        Assignment assignment = assignmentRepository.findByIdWithDetails(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        
        return AssignmentDetailResponse.from(assignment);
    }
    
    /**
     * 과제 파일 다운로드 URL 생성
     */
    public String getAssignmentFileDownloadUrl(Long assignmentId, Long fileId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        
        AssignmentFile file = assignment.getFiles().stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
        
        // S3 Pre-signed URL 생성 (1시간 유효)
        return s3Service.generatePresignedUrl(file.getStoredFileName());
    }
    
    /**
     * 과제 수정
     */
    @Transactional
    public AssignmentDetailResponse updateAssignment(Long assignmentId, AssignmentUpdateRequest request, Long teacherId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        
        // 권한 확인
        if (!assignment.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        
        // 발행된 과제는 일부 항목만 수정 가능
        if (assignment.getStatus() != Assignment.AssignmentStatus.DRAFT) {
            // 마감일, 늦은 제출 허용 여부만 수정 가능
            if (request.getDueDate() != null) {
                assignment = Assignment.builder()
                        .id(assignment.getId())
                        .title(assignment.getTitle())
                        .description(assignment.getDescription())
                        .teacherId(assignment.getTeacherId())
                        .dueDate(request.getDueDate())
                        .status(assignment.getStatus())
                        .maxScore(assignment.getMaxScore())
                        .allowLateSubmission(request.getAllowLateSubmission())
                        .build();
            }
        } else {
            // 초안 상태에서는 모든 항목 수정 가능
            assignment = Assignment.builder()
                    .id(assignment.getId())
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .teacherId(assignment.getTeacherId())
                    .dueDate(request.getDueDate())
                    .status(assignment.getStatus())
                    .maxScore(request.getMaxScore())
                    .allowLateSubmission(request.getAllowLateSubmission())
                    .build();
        }
        
        Assignment updated = assignmentRepository.save(assignment);
        return AssignmentDetailResponse.from(updated);
    }
    
    /**
     * 과제 삭제
     */
    @Transactional
    public void deleteAssignment(Long assignmentId, Long teacherId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        
        // 권한 확인
        if (!assignment.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        
        // 발행된 과제는 삭제 불가
        if (assignment.getStatus() != Assignment.AssignmentStatus.DRAFT) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        
        // S3 파일 삭제
        List<String> s3Keys = assignment.getFiles().stream()
                .map(AssignmentFile::getStoredFileName)
                .toList();
        
        if (!s3Keys.isEmpty()) {
            s3Service.deleteMultiple(s3Keys);
        }
        
        // 과제 삭제
        assignmentRepository.delete(assignment);
        
        log.info("과제 삭제 완료: id={}", assignmentId);
    }
}
package com.pullit.exam.service;


import com.pullit.classes.entity.Classes;
import com.pullit.classes.repository.ClassRepository;
import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;
import com.pullit.student.repository.StudentRepository;
import com.pullit.exam.dto.request.ExamAssignmentRequest;
import com.pullit.exam.dto.response.ExamAssignmentResponse;
import com.pullit.exam.entity.Exam;
import com.pullit.exam.entity.UserExam;
import com.pullit.exam.entity.ExamAssignment;
import com.pullit.exam.repository.ExamAssignmentRepository;
import com.pullit.exam.repository.ExamRepository;
import com.pullit.exam.repository.UserExamRepository;
import com.pullit.notification.annotation.NotificationTrigger;
import com.pullit.notification.enums.NotificationType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 시험 출제 서비스
 * 시험을 여러 학급에 배정하고 알림을 발송하는 기능을 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamAssignmentService {

    private final ExamAssignmentRepository examAssignmentRepository;
    private final ExamRepository examRepository;
    private final UserExamRepository userExamRepository;
    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;

    /**
     * 시험을 여러 학급에 출제합니다.
     * @NotificationTrigger 어노테이션을 통해 학생들에게 자동으로 알림이 발송됩니다.
     */
    @Transactional
    @NotificationTrigger(
        type = NotificationType.EXAM_ASSIGNED,
        multipleUsers = true,
        userIdsExpression = "#result.assignedClasses.![studentCount > 0 ? classId : null]",
        title = "'시험 출제 알림'",
        message = "'「' + #result.examName + '」 시험이 ' + #result.examDate + ' ' + #result.examTime + '에 예정되어 있습니다.'",
        targetUrl = "'/exams/' + #result.examId",
        condition = "#request.sendNotification == true"
    )
    public ExamAssignmentResponse assignExamToClasses(ExamAssignmentRequest request) {
        log.info("시험 출제 요청 시작 - examId: {}, classIds: {}", request.getExamId(), request.getClassIds());

        // 1. 요청 유효성 검증
        validateRequest(request);

        // 2. 시험 조회 (UserExam에서 찾기)
        UserExam userExam = userExamRepository.findById(request.getExamId())
                .orElseThrow(() -> new BusinessException(ErrorCode.EXAM_NOT_FOUND, "시험을 찾을 수 없습니다. ID: " + request.getExamId()));

        // 3. 학급 조회 및 검증
        List<Classes> classes = classRepository.findAllById(request.getClassIds());
        if (classes.size() != request.getClassIds().size()) {
            throw new BusinessException(ErrorCode.CLASS_NOT_FOUND, "일부 학급을 찾을 수 없습니다.");
        }

        // 4. 중복 출제 확인
        checkDuplicateAssignments(request.getExamId(), request.getClassIds());

        // 5. 시험 출제 정보 생성
        List<ExamAssignment> assignments = new ArrayList<>();
        for (Classes classEntity : classes) {
            ExamAssignment assignment = createExamAssignmentForUserExam(userExam, classEntity, request);
            assignments.add(assignment);
            log.debug("학급 {}에 시험 출제 준비 완료", classEntity.getClassName());
        }

        // 6. 시험 출제 정보 저장
        List<ExamAssignment> savedAssignments = examAssignmentRepository.saveAll(assignments);
        log.info("{}개 학급에 시험 출제 완료", savedAssignments.size());

        // 7. 알림 발송 (NotificationTrigger 어노테이션이 자동 처리)
        if (request.getSendNotification()) {
            // 알림 발송을 위한 학생 ID 목록 수집
            List<Long> studentIds = collectStudentIds(classes);
            log.info("{}명의 학생에게 알림 발송 예정", studentIds.size());
            
            // 출제 정보에 알림 발송 상태 업데이트
            savedAssignments.forEach(assignment -> {
                assignment.markNotificationSent();
            });
        }

        // 8. 응답 생성
        ExamAssignmentResponse response = ExamAssignmentResponse.fromMultiple(savedAssignments);
        response.setMessage(generateSuccessMessage(savedAssignments.size(), userExam.getExamName()));
        
        return response;
    }

    /**
     * 요청 유효성 검증
     */
    private void validateRequest(ExamAssignmentRequest request) {
        if (!request.isValid()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "시험 날짜와 시간이 유효하지 않습니다.");
        }

        if (request.getClassIds() == null || request.getClassIds().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "최소 하나 이상의 학급을 선택해야 합니다.");
        }

        if (request.getTimeLimit() < 10 || request.getTimeLimit() > 300) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "시험 시간은 10분 이상 300분 이하로 설정해야 합니다.");
        }
    }

    /**
     * 중복 출제 확인
     */
    private void checkDuplicateAssignments(Long userExamId, List<Long> classIds) {
        for (Long classId : classIds) {
            if (examAssignmentRepository.existsActiveAssignment(userExamId, classId)) {
                throw new BusinessException(ErrorCode.DUPLICATE_ASSIGNMENT, 
                    String.format("학급 ID %d에 이미 해당 시험이 출제되어 있습니다.", classId));
            }
        }
    }

    /**
     * ExamAssignment 엔티티 생성
     */
    private ExamAssignment createExamAssignmentForUserExam(UserExam userExam, Classes classEntity, ExamAssignmentRequest request) {
        // UserExam을 직접 사용
        return ExamAssignment.builder()
                .userExam(userExam)
                .classEntity(classEntity)
                .examDate(request.getExamDate())
                .examTime(request.getExamTime())
                .timeLimit(request.getTimeLimit())
                .maxAttempts(request.getMaxAttempts())
                .allowReview(request.getAllowReview())
                .showAnswer(request.getShowAnswer())
                .randomOrder(request.getRandomOrder())
                .status(ExamAssignment.ExamAssignmentStatus.SCHEDULED)
                .notificationSent(false)
                .build();
    }

    /**
     * 학급에 속한 학생 ID 목록 수집
     * Student 엔티티의 classGroupID로 해당 학급에 속한 학생들을 조회
     */
    private List<Long> collectStudentIds(List<Classes> classes) {
        return classes.stream()
                .flatMap(classEntity -> {
                    // Student 엔티티에서 classGroupID로 학생들 조회
                    return studentRepository.findByClassGroupID(classEntity.getClassId()).stream()
                            .map(student -> student.getUserId());
                })
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 성공 메시지 생성
     */
    private String generateSuccessMessage(int classCount, String examName) {
        return String.format("「%s」 시험이 %d개 학급에 성공적으로 출제되었습니다.", examName, classCount);
    }

    /**
     * 특정 학급의 예정된 시험 조회
     */
    public List<ExamAssignmentResponse> getUpcomingExamsByClassId(Long classId) {
        List<ExamAssignment> assignments = examAssignmentRepository.findUpcomingExamsByClassId(
                classId, LocalDateTime.now());
        
        return assignments.stream()
                .map(ExamAssignmentResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 시험의 출제 정보 조회
     */
    public List<ExamAssignmentResponse> getAssignmentsByExamId(Long userExamId) {
        List<ExamAssignment> assignments = examAssignmentRepository.findByUserExamId(userExamId);
        
        if (assignments.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 같은 시험의 여러 출제 정보를 하나로 묶어서 반환
        return List.of(ExamAssignmentResponse.fromMultiple(assignments));
    }

    /**
     * 시험 출제 취소
     */
    @Transactional
    public void cancelAssignment(Long assignmentId) {
        ExamAssignment assignment = examAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSIGNMENT_NOT_FOUND, 
                    "시험 출제 정보를 찾을 수 없습니다. ID: " + assignmentId));
        
        if (assignment.getStatus() != ExamAssignment.ExamAssignmentStatus.SCHEDULED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "예정된 시험만 취소할 수 있습니다.");
        }
        
        assignment.cancel();
        log.info("시험 출제 취소 완료 - assignmentId: {}", assignmentId);
    }
}
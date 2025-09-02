package com.pullit.exam.service;

import com.pullit.classes.entity.Classes;
import com.pullit.classes.repository.ClassRepository;
import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;
import com.pullit.exam.dto.request.TeacherLiveExamRequest;
import com.pullit.exam.dto.response.TeacherLiveExamResponse;
import com.pullit.exam.entity.TeacherLiveExam;
import com.pullit.exam.entity.TeacherLiveExamItem;
import com.pullit.exam.entity.UserExamItem;
import com.pullit.exam.entity.UserExam;
import com.pullit.exam.repository.TeacherLiveExamRepository;
import com.pullit.exam.repository.UserExamItemRepository;
import com.pullit.exam.repository.UserExamRepository;
import com.pullit.notification.service.NotificationService;
import com.pullit.user.entity.User;
import com.pullit.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherLiveExamService {
    
    private final TeacherLiveExamRepository teacherLiveExamRepository;
    private final UserExamItemRepository userExamItemRepository;
    private final UserExamRepository userExamRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    
    /**
     * 선생님이 실시간 시험 생성
     */
    @Transactional
    public TeacherLiveExamResponse createLiveExam(Long teacherId, TeacherLiveExamRequest request) {
        log.info("Creating live exam for teacher: {}, class: {}", teacherId, request.getClassId());
        
        // 선생님 확인
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        // 클래스 확인
        Classes examClass = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CLASS_NOT_FOUND));
        
        // 선생님이 해당 클래스의 담당인지 확인
        if (!examClass.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        // 원본 시험지 기반 채우기 (sourceExamId 우선)
        UserExam sourceExam = null;
        if (request.getSourceExamId() != null) {
            sourceExam = userExamRepository.findById(request.getSourceExamId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.EXAM_NOT_FOUND));
            // 동일 원본 시험으로 이미 생성된 실시간 시험이 있으면 재사용 (중복 방지)
            List<TeacherLiveExam> existing = teacherLiveExamRepository.findRecentBySourceExamAndClass(
                    sourceExam.getId(), request.getClassId());
            if (existing != null && !existing.isEmpty()) {
                log.info("Reusing existing live exam: {}", existing.get(0).getId());
                return TeacherLiveExamResponse.from(existing.get(0));
            }
        }

        // 실시간 시험 엔티티 생성
        TeacherLiveExam liveExam = TeacherLiveExam.builder()
                .examName(request.getExamName() != null ? request.getExamName() : (sourceExam != null ? sourceExam.getExamName() : null))
                .examClass(examClass)
                .teacher(teacher)
                .timeLimit(request.getTimeLimit() != null ? request.getTimeLimit() : (sourceExam != null ? sourceExam.getTimeLimit() : null))
                .scheduledDate(request.getScheduledDate())
                .scheduledTime(request.getScheduledTime())
                .description(request.getDescription())
                .gradeCode(request.getGradeCode() != null ? request.getGradeCode() : (sourceExam != null ? sourceExam.getGradeCode() : null))
                .termCode(request.getTermCode() != null ? request.getTermCode() : (sourceExam != null ? sourceExam.getTermCode() : null))
                .subjectCode(request.getSubjectCode() != null ? request.getSubjectCode() : (sourceExam != null ? sourceExam.getAreaCode() : null))
                .build();
        
        // 문제 추가
        if (request.getExamItemIds() != null && !request.getExamItemIds().isEmpty()) {
            int order = 1;
            for (Long itemId : request.getExamItemIds()) {
                UserExamItem examItem = userExamItemRepository.findById(itemId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
                
                TeacherLiveExamItem liveExamItem = TeacherLiveExamItem.builder()
                        .liveExam(liveExam)
                        .userExamItem(examItem)
                        .itemOrder(order++)
                        .points(examItem.getPoints())
                        .build();
                
                liveExam.addExamItem(liveExamItem);
            }
        }
        // sourceExamId가 있는 경우 해당 시험의 모든 문항을 추가
        else if (sourceExam != null) {
            List<UserExamItem> sourceItems = userExamItemRepository.findByUserExamIdOrderByItemOrder(sourceExam.getId());
            int order = 1;
            for (UserExamItem examItem : sourceItems) {
                TeacherLiveExamItem liveExamItem = TeacherLiveExamItem.builder()
                        .liveExam(liveExam)
                        .userExamItem(examItem)
                        .itemOrder(order++)
                        .points(examItem.getPoints())
                        .build();
                liveExam.addExamItem(liveExamItem);
            }
        }
        
        // 저장
        TeacherLiveExam savedExam = teacherLiveExamRepository.save(liveExam);
        
        // WebSocket으로 실시간 알림 전송
        sendLiveExamNotification(examClass.getClassId(), savedExam, "EXAM_CREATED");
        
        // 알림 저장
        notificationService.createExamNotification(
                examClass.getClassId(),
                savedExam.getId(),
                "LIVE_EXAM",
                "EXAM_CREATED",
                String.format("새로운 실시간 시험 '%s'이(가) 생성되었습니다.", savedExam.getExamName())
        );
        
        log.info("Live exam created successfully: {}", savedExam.getId());
        return TeacherLiveExamResponse.from(savedExam);
    }
    
    /**
     * 클래스의 실시간 시험 목록 조회
     */
    public List<TeacherLiveExamResponse> getClassLiveExams(Long classId) {
        List<TeacherLiveExam> exams = teacherLiveExamRepository.findByClassId(classId);
        return exams.stream()
                .map(TeacherLiveExamResponse::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 클래스의 활성 시험 목록 조회
     */
    public List<TeacherLiveExamResponse> getActiveExams(Long classId) {
        List<TeacherLiveExam> exams = teacherLiveExamRepository.findActiveExamsByClassId(classId);
        return exams.stream()
                .map(TeacherLiveExamResponse::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 시험 시작
     */
    @Transactional
    public TeacherLiveExamResponse startExam(Long examId, Long teacherId) {
        TeacherLiveExam exam = teacherLiveExamRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXAM_NOT_FOUND));
        
        // 권한 확인
        if (!exam.getTeacher().getId().equals(teacherId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        // 시험 시작
        if (!exam.canStart()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }
        
        exam.startExam();
        TeacherLiveExam savedExam = teacherLiveExamRepository.save(exam);
        
        // WebSocket으로 실시간 알림 전송
        sendLiveExamNotification(exam.getExamClass().getClassId(), savedExam, "EXAM_STARTED");
        
        // 알림 저장
        notificationService.createExamNotification(
                exam.getExamClass().getClassId(),
                savedExam.getId(),
                "LIVE_EXAM",
                "EXAM_STARTED",
                String.format("실시간 시험 '%s'이(가) 시작되었습니다!", savedExam.getExamName())
        );
        
        log.info("Live exam started: {}", examId);
        return TeacherLiveExamResponse.from(savedExam);
    }
    
    /**
     * 시험 종료
     */
    @Transactional
    public TeacherLiveExamResponse endExam(Long examId, Long teacherId) {
        TeacherLiveExam exam = teacherLiveExamRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXAM_NOT_FOUND));
        
        // 권한 확인
        if (!exam.getTeacher().getId().equals(teacherId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        exam.endExam();
        TeacherLiveExam savedExam = teacherLiveExamRepository.save(exam);
        
        // WebSocket으로 실시간 알림 전송
        sendLiveExamNotification(exam.getExamClass().getClassId(), savedExam, "EXAM_ENDED");
        
        // 알림 저장
        notificationService.createExamNotification(
                exam.getExamClass().getClassId(),
                savedExam.getId(),
                "LIVE_EXAM",
                "EXAM_ENDED",
                String.format("실시간 시험 '%s'이(가) 종료되었습니다.", savedExam.getExamName())
        );
        
        log.info("Live exam ended: {}", examId);
        return TeacherLiveExamResponse.from(savedExam);
    }
    
    /**
     * WebSocket으로 실시간 알림 전송
     */
    private void sendLiveExamNotification(Long classId, TeacherLiveExam exam, String eventType) {
        String destination = "/topic/class-" + classId + "/exam-status";
        
        TeacherLiveExamResponse response = TeacherLiveExamResponse.from(exam);
        response.setEventType(eventType);
        
        messagingTemplate.convertAndSend(destination, response);
        log.info("Sent live exam notification to {}: {}", destination, eventType);
    }
    
    /**
     * 오늘 예정된 시험 조회
     */
    public List<TeacherLiveExamResponse> getTodaysExams(Long classId) {
        LocalDate today = LocalDate.now();
        List<TeacherLiveExam> exams = teacherLiveExamRepository.findTodaysExamsByClassId(classId, today);
        return exams.stream()
                .map(TeacherLiveExamResponse::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 현재 진행 중인 시험 조회
     */
    public TeacherLiveExamResponse getCurrentExam(Long classId) {
        return teacherLiveExamRepository.findCurrentExamByClassId(classId)
                .map(TeacherLiveExamResponse::from)
                .orElse(null);
    }
}

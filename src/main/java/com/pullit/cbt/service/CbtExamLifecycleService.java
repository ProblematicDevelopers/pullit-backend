package com.pullit.cbt.service;

import com.pullit.cbt.dto.response.CbtExamLifecycleResponse;
import com.pullit.notification.annotation.NotificationTrigger;
import com.pullit.notification.enums.NotificationType;
import com.pullit.classes.entity.Classes;
import com.pullit.classes.repository.ClassRepository;
import com.pullit.exam.entity.UserExam;
import com.pullit.exam.repository.UserExamRepository;
import com.pullit.student.entity.Student;
import com.pullit.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CbtExamLifecycleService {
    
    private final UserExamRepository userExamRepository;
    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * CBT 시험 시작 - 학급의 모든 학생에게 알림 발송
     */
    @NotificationTrigger(
        type = NotificationType.EXAM_STARTED,
        multipleUsers = true,
        userIdsExpression = "#result.studentIds",
        message = "'실시간 시험 [' + #result.examName + ']이(가) 시작되었습니다. 지금 응시해주세요.'",
        targetUrl = "'/student/exam/cbt/' + #result.examId"
    )
    public CbtExamLifecycleResponse startCbtExam(Long examId, Long classId, Long teacherId) {
        log.info("Starting CBT exam: examId={}, classId={}, teacherId={}", examId, classId, teacherId);
        
        // 시험 정보 조회
        UserExam exam = userExamRepository.findById(examId)
            .orElseThrow(() -> new IllegalArgumentException("시험을 찾을 수 없습니다: " + examId));
        
        // 학급 정보 조회
        Classes classRoom = classRepository.findById(classId)
            .orElseThrow(() -> new IllegalArgumentException("학급을 찾을 수 없습니다: " + classId));
        
        // 학급 학생 목록 조회
        List<Long> studentIds = getClassStudentIds(classRoom);
        
        log.info("Sending exam start notification to {} students", studentIds.size());
        
        // STOMP 브로드캐스트: 학급 채널에 시험 시작 이벤트 전송 (학생 대시보드 배너/리스트용)
        broadcastExamEvent(classId, buildExamEventPayload(exam, classRoom, "EXAM_STARTED"));
        
        return CbtExamLifecycleResponse.builder()
            .examId(examId)
            .examName(exam.getExamName())
            .classId(classId)
            .className(classRoom.getClassName())
            .studentIds(studentIds)
            .status("STARTED")
            .startTime(LocalDateTime.now())
            .message("시험이 시작되었습니다")
            .build();
    }
    
    /**
     * CBT 시험 종료 - 학급의 모든 학생에게 알림 발송
     */
    @NotificationTrigger(
        type = NotificationType.EXAM_ENDED,
        multipleUsers = true,
        userIdsExpression = "#result.studentIds",
        message = "'실시간 시험 [' + #result.examName + ']이(가) 종료되었습니다.'",
        targetUrl = "'/student/exam/results/' + #result.examId"
    )
    public CbtExamLifecycleResponse endCbtExam(Long examId, Long classId, Long teacherId) {
        log.info("Ending CBT exam: examId={}, classId={}, teacherId={}", examId, classId, teacherId);
        
        // 시험 정보 조회
        UserExam exam = userExamRepository.findById(examId)
            .orElseThrow(() -> new IllegalArgumentException("시험을 찾을 수 없습니다: " + examId));
        
        // 학급 정보 조회
        Classes classRoom = classRepository.findById(classId)
            .orElseThrow(() -> new IllegalArgumentException("학급을 찾을 수 없습니다: " + classId));
        
        // 학급 학생 목록 조회
        List<Long> studentIds = getClassStudentIds(classRoom);
        
        log.info("Sending exam end notification to {} students", studentIds.size());
        
        // STOMP 브로드캐스트: 학급 채널에 시험 종료 이벤트 전송 (학생 대시보드 배너 업데이트용)
        broadcastExamEvent(classId, buildExamEventPayload(exam, classRoom, "EXAM_ENDED"));
        
        return CbtExamLifecycleResponse.builder()
            .examId(examId)
            .examName(exam.getExamName())
            .classId(classId)
            .className(classRoom.getClassName())
            .studentIds(studentIds)
            .status("ENDED")
            .endTime(LocalDateTime.now())
            .message("시험이 종료되었습니다")
            .build();
    }
    
    /**
     * 학급의 학생 ID 목록 조회
     */
    private List<Long> getClassStudentIds(Classes classRoom) {
        // Student 테이블에서 classGroupID로 학생 조회
        List<Student> students = studentRepository.findByClassGroupID(classRoom.getClassId());
        return students.stream()
            .map(Student::getUserId)
            .collect(Collectors.toList());
    }
    
    /**
     * 학급 채널로 시험 상태 이벤트 브로드캐스트
     * 프런트의 클래스 대시보드(`class-{classId}` 채널)에서 수신하여 배너/리스트를 갱신합니다.
     */
    private void broadcastExamEvent(Long classId, Map<String, Object> payload) {
        try {
            String destination = "/topic/class-" + classId + "/exam-status";
            messagingTemplate.convertAndSend(destination, payload);
            log.info("Broadcasted CBT exam event to {}: {}", destination, payload.get("eventType"));
        } catch (Exception e) {
            log.error("Failed to broadcast CBT exam event for class {}", classId, e);
        }
    }
    
    /**
     * 프런트에서 기대하는 필드 형태로 시험 이벤트 페이로드 생성
     */
    private Map<String, Object> buildExamEventPayload(UserExam exam, Classes classRoom, String eventType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", exam.getId());               // 프런트에서 라우팅에 사용
        payload.put("examId", exam.getId());           // 배너 이동 등에 사용
        payload.put("examName", exam.getExamName());
        payload.put("classId", classRoom.getClassId());
        payload.put("className", classRoom.getClassName());
        payload.put("examType", "CBT");
        payload.put("examStatus", eventType.equals("EXAM_ENDED") ? "ENDED" : "STARTED");
        payload.put("totalItems", exam.getTotalItems());
        payload.put("totalPoints", exam.getTotalPoints());
        payload.put("timeLimit", exam.getTimeLimit());
        payload.put("areaName", exam.getAreaName());
        payload.put("eventType", eventType);
        return payload;
    }
}

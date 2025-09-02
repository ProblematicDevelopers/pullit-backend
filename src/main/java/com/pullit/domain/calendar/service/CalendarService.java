package com.pullit.domain.calendar.service;

import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;
import com.pullit.domain.assignment.entity.Assignment;
import com.pullit.domain.assignment.entity.Submission;
import com.pullit.domain.assignment.repository.AssignmentRepository;
import com.pullit.domain.calendar.dto.CalendarEventRequest;
import com.pullit.domain.calendar.dto.CalendarEventResponse;
import com.pullit.domain.calendar.entity.CalendarEvent;
import com.pullit.domain.calendar.repository.CalendarEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {
    
    private final CalendarEventRepository calendarEventRepository;
    private final AssignmentRepository assignmentRepository;
    
    /**
     * 캘린더 이벤트 생성
     */
    @Transactional
    public CalendarEventResponse createEvent(CalendarEventRequest request) {
        CalendarEvent event = CalendarEvent.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startDateTime(request.getStartDateTime())
                .endDateTime(request.getEndDateTime())
                .allDay(request.getAllDay())
                .userId(request.getUserId())
                .eventType(request.getEventType())
                .color(request.getColor())
                .relatedId(request.getRelatedId())
                .status(CalendarEvent.EventStatus.SCHEDULED)
                .location(request.getLocation())
                .reminder(request.getReminder())
                .reminderMinutes(request.getReminderMinutes())
                .build();
        
        CalendarEvent saved = calendarEventRepository.save(event);
        log.info("캘린더 이벤트 생성: id={}, title={}", saved.getId(), saved.getTitle());
        
        return CalendarEventResponse.from(saved);
    }
    
    /**
     * 과제 생성 시 자동으로 캘린더에 추가
     */
    @Transactional
    @Async
    public void createAssignmentEvent(Assignment assignment, List<Long> studentIds) {
        // 각 학생의 캘린더에 과제 마감일 추가
        for (Long studentId : studentIds) {
            try {
                CalendarEvent event = CalendarEvent.builder()
                        .title("[과제] " + assignment.getTitle())
                        .description(assignment.getDescription())
                        .startDateTime(assignment.getDueDate().minusHours(1)) // 마감 1시간 전 시작
                        .endDateTime(assignment.getDueDate())
                        .allDay(false)
                        .userId(studentId)
                        .eventType(CalendarEvent.EventType.ASSIGNMENT)
                        .color("#ff9800") // 주황색
                        .relatedId(assignment.getId())
                        .status(CalendarEvent.EventStatus.SCHEDULED)
                        .reminder(true)
                        .reminderMinutes(60) // 1시간 전 알림
                        .build();
                
                calendarEventRepository.save(event);
                log.info("과제 캘린더 이벤트 생성: assignmentId={}, studentId={}", 
                        assignment.getId(), studentId);
            } catch (Exception e) {
                log.error("과제 캘린더 이벤트 생성 실패: assignmentId={}, studentId={}", 
                        assignment.getId(), studentId, e);
            }
        }
    }
    
    /**
     * 과제 제출 시 캘린더 이벤트 업데이트
     */
    @Transactional
    public void updateAssignmentEventStatus(Long assignmentId, Long studentId) {
        calendarEventRepository.findByRelatedIdAndEventType(assignmentId, CalendarEvent.EventType.ASSIGNMENT)
                .ifPresent(event -> {
                    if (event.getUserId().equals(studentId)) {
                        event.updateStatus(CalendarEvent.EventStatus.COMPLETED);
                        // 색상도 녹색으로 변경
                        event.updateEvent(null, null, null, null, "#4caf50");
                        calendarEventRepository.save(event);
                        log.info("과제 캘린더 이벤트 완료 처리: eventId={}", event.getId());
                    }
                });
    }
    
    /**
     * 사용자의 특정 기간 이벤트 조회
     */
    public List<CalendarEventResponse> getUserEvents(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        List<CalendarEvent> events = calendarEventRepository.findByUserIdAndDateRange(
                userId, startDateTime, endDateTime);
        
        return events.stream()
                .map(CalendarEventResponse::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 사용자의 오늘 일정 조회
     */
    public List<CalendarEventResponse> getTodayEvents(Long userId) {
        List<CalendarEvent> events = calendarEventRepository.findTodayEvents(
                userId, LocalDateTime.now());
        
        return events.stream()
                .map(CalendarEventResponse::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 사용자의 예정된 이벤트 조회
     */
    public List<CalendarEventResponse> getUpcomingEvents(Long userId) {
        List<CalendarEvent> events = calendarEventRepository.findUpcomingEvents(
                userId, LocalDateTime.now());
        
        return events.stream()
                .map(CalendarEventResponse::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 이벤트 수정
     */
    @Transactional
    public CalendarEventResponse updateEvent(Long eventId, CalendarEventRequest request, Long userId) {
        CalendarEvent event = calendarEventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        
        // 권한 확인
        if (!event.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        
        event.updateEvent(
                request.getTitle(),
                request.getDescription(),
                request.getStartDateTime(),
                request.getEndDateTime(),
                request.getColor()
        );
        
        CalendarEvent updated = calendarEventRepository.save(event);
        return CalendarEventResponse.from(updated);
    }
    
    /**
     * 이벤트 삭제
     */
    @Transactional
    public void deleteEvent(Long eventId, Long userId) {
        CalendarEvent event = calendarEventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        
        // 권한 확인
        if (!event.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        
        calendarEventRepository.delete(event);
        log.info("캘린더 이벤트 삭제: id={}", eventId);
    }
    
    /**
     * 과제 관련 이벤트 조회
     */
    public List<CalendarEventResponse> getAssignmentEvents(Long userId) {
        List<CalendarEvent> events = calendarEventRepository.findByUserIdAndEventType(
                userId, CalendarEvent.EventType.ASSIGNMENT);
        
        return events.stream()
                .map(CalendarEventResponse::from)
                .collect(Collectors.toList());
    }
}
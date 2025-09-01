package com.pullit.schedule.service;

import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;
import com.pullit.schedule.dto.request.ScheduleCreateRequest;
import com.pullit.schedule.dto.request.ScheduleUpdateRequest;
import com.pullit.schedule.dto.response.ScheduleResponse;
import com.pullit.schedule.entity.Schedule;
import com.pullit.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ScheduleService {
    
    private final ScheduleRepository scheduleRepository;
    
    // 일정 생성
    public ScheduleResponse createSchedule(Long userId, ScheduleCreateRequest request) {
        
        Schedule schedule = Schedule.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getType())
                .scheduledDate(request.getScheduledDate())
                .endDate(request.getEndDate())
                .location(request.getLocation())
                .participants(request.getParticipants())
                .classId(request.getClassId())
                .examId(request.getExamId())
                .isRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false)
                .recurrencePattern(request.getRecurrencePattern())
                .reminderEnabled(request.getReminderEnabled() != null ? request.getReminderEnabled() : false)
                .reminderMinutes(request.getReminderMinutes())
                .color(request.getColor() != null ? request.getColor() : "#2563eb")
                .status("upcoming")
                .build();
        
        // createdBy 수동 설정 (JPA Auditing이 작동하지 않을 경우를 대비)
        schedule.setCreatedBy(userId);
        Schedule saved = scheduleRepository.save(schedule);
        
        log.info("일정 생성 완료: scheduleId={}, title={}", saved.getId(), saved.getTitle());
        return ScheduleResponse.from(saved);
    }
    
    // 일정 조회
    @Transactional(readOnly = true)
    public ScheduleResponse getSchedule(Long scheduleId, Long userId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "일정을 찾을 수 없습니다."));
        
        // 권한 체크
        if (!schedule.getCreatedBy().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "해당 일정에 대한 권한이 없습니다.");
        }
        
        return ScheduleResponse.from(schedule);
    }
    
    // 일정 수정
    public ScheduleResponse updateSchedule(Long scheduleId, Long userId, ScheduleUpdateRequest request) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "일정을 찾을 수 없습니다."));
        
        // 권한 체크
        if (!schedule.getCreatedBy().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "해당 일정에 대한 권한이 없습니다.");
        }
        
        // 수정 가능한 필드만 업데이트
        if (request.getTitle() != null) {
            schedule.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            schedule.setDescription(request.getDescription());
        }
        if (request.getType() != null) {
            schedule.setType(request.getType());
        }
        if (request.getScheduledDate() != null) {
            schedule.setScheduledDate(request.getScheduledDate());
        }
        if (request.getEndDate() != null) {
            schedule.setEndDate(request.getEndDate());
        }
        if (request.getLocation() != null) {
            schedule.setLocation(request.getLocation());
        }
        if (request.getParticipants() != null) {
            schedule.setParticipants(request.getParticipants());
        }
        if (request.getIsRecurring() != null) {
            schedule.setIsRecurring(request.getIsRecurring());
        }
        if (request.getRecurrencePattern() != null) {
            schedule.setRecurrencePattern(request.getRecurrencePattern());
        }
        if (request.getReminderEnabled() != null) {
            schedule.setReminderEnabled(request.getReminderEnabled());
        }
        if (request.getReminderMinutes() != null) {
            schedule.setReminderMinutes(request.getReminderMinutes());
        }
        if (request.getColor() != null) {
            schedule.setColor(request.getColor());
        }
        
        Schedule updated = scheduleRepository.save(schedule);
        log.info("일정 수정 완료: scheduleId={}", scheduleId);
        
        return ScheduleResponse.from(updated);
    }
    
    // 일정 삭제
    public void deleteSchedule(Long scheduleId, Long userId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "일정을 찾을 수 없습니다."));
        
        // 권한 체크
        if (!schedule.getCreatedBy().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "해당 일정에 대한 권한이 없습니다.");
        }
        
        schedule.softDelete(userId);
        scheduleRepository.save(schedule);
        
        log.info("일정 삭제 완료: scheduleId={}", scheduleId);
    }
    
    // 월별 일정 조회
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getMonthlySchedules(Long userId, int year, int month) {
        
        List<Schedule> schedules = scheduleRepository.findByMonth(userId, year, month);
        
        return schedules.stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }
    
    // 날짜 범위로 일정 조회
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getSchedulesByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        List<Schedule> schedules = scheduleRepository.findByDateRange(userId, startDateTime, endDateTime);
        
        return schedules.stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }
    
    // 일정 상태 변경
    public ScheduleResponse updateScheduleStatus(Long scheduleId, Long userId, String status) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "일정을 찾을 수 없습니다."));
        
        // 권한 체크
        if (!schedule.getCreatedBy().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "해당 일정에 대한 권한이 없습니다.");
        }
        
        // 유효한 상태값 체크
        if (!List.of("upcoming", "ongoing", "completed", "cancelled").contains(status)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지 않은 상태값입니다.");
        }
        
        schedule.updateStatus(status);
        Schedule updated = scheduleRepository.save(schedule);
        
        log.info("일정 상태 변경 완료: scheduleId={}, status={}", scheduleId, status);
        
        return ScheduleResponse.from(updated);
    }
}
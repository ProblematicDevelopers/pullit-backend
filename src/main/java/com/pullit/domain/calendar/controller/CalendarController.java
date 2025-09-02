package com.pullit.domain.calendar.controller;

import com.pullit.domain.calendar.dto.CalendarEventRequest;
import com.pullit.domain.calendar.dto.CalendarEventResponse;
import com.pullit.domain.calendar.service.CalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
@Tag(name = "Calendar", description = "캘린더 관리 API")
public class CalendarController {
    
    private final CalendarService calendarService;
    
    @PostMapping("/events")
    @Operation(summary = "캘린더 이벤트 생성", description = "새로운 캘린더 이벤트를 생성합니다")
    public ResponseEntity<CalendarEventResponse> createEvent(
            @Valid @RequestBody CalendarEventRequest request) {
        
        log.info("캘린더 이벤트 생성 요청: title={}, userId={}", request.getTitle(), request.getUserId());
        CalendarEventResponse response = calendarService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/events")
    @Operation(summary = "캘린더 이벤트 조회", description = "특정 기간의 캘린더 이벤트를 조회합니다")
    public ResponseEntity<List<CalendarEventResponse>> getUserEvents(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("캘린더 이벤트 조회: userId={}, period={} ~ {}", userId, startDate, endDate);
        List<CalendarEventResponse> events = calendarService.getUserEvents(userId, startDate, endDate);
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/events/today")
    @Operation(summary = "오늘 일정 조회", description = "사용자의 오늘 일정을 조회합니다")
    public ResponseEntity<List<CalendarEventResponse>> getTodayEvents(@RequestParam Long userId) {
        log.info("오늘 일정 조회: userId={}", userId);
        List<CalendarEventResponse> events = calendarService.getTodayEvents(userId);
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/events/upcoming")
    @Operation(summary = "예정된 일정 조회", description = "사용자의 예정된 일정을 조회합니다")
    public ResponseEntity<List<CalendarEventResponse>> getUpcomingEvents(@RequestParam Long userId) {
        log.info("예정된 일정 조회: userId={}", userId);
        List<CalendarEventResponse> events = calendarService.getUpcomingEvents(userId);
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/events/assignments")
    @Operation(summary = "과제 일정 조회", description = "사용자의 과제 관련 일정을 조회합니다")
    public ResponseEntity<List<CalendarEventResponse>> getAssignmentEvents(@RequestParam Long userId) {
        log.info("과제 일정 조회: userId={}", userId);
        List<CalendarEventResponse> events = calendarService.getAssignmentEvents(userId);
        return ResponseEntity.ok(events);
    }
    
    @PutMapping("/events/{eventId}")
    @Operation(summary = "캘린더 이벤트 수정", description = "캘린더 이벤트를 수정합니다")
    public ResponseEntity<CalendarEventResponse> updateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody CalendarEventRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("캘린더 이벤트 수정: eventId={}, userId={}", eventId, userId);
        CalendarEventResponse response = calendarService.updateEvent(eventId, request, userId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/events/{eventId}")
    @Operation(summary = "캘린더 이벤트 삭제", description = "캘린더 이벤트를 삭제합니다")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Long eventId,
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("캘린더 이벤트 삭제: eventId={}, userId={}", eventId, userId);
        calendarService.deleteEvent(eventId, userId);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/events/assignment/{assignmentId}/complete")
    @Operation(summary = "과제 완료 처리", description = "과제 제출 시 캘린더 이벤트를 완료 처리합니다")
    public ResponseEntity<Void> completeAssignmentEvent(
            @PathVariable Long assignmentId,
            @RequestParam Long studentId) {
        
        log.info("과제 캘린더 이벤트 완료 처리: assignmentId={}, studentId={}", assignmentId, studentId);
        calendarService.updateAssignmentEventStatus(assignmentId, studentId);
        return ResponseEntity.ok().build();
    }
}
package com.pullit.domain.calendar.entity;

import com.pullit.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "calendar_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CalendarEvent extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private LocalDateTime startDateTime;
    
    @Column(nullable = false)
    private LocalDateTime endDateTime;
    
    @Column
    private Boolean allDay;
    
    @Column(nullable = false)
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;
    
    @Column
    private String color;
    
    @Column
    private Long relatedId; // 관련 엔티티 ID (assignmentId, examId 등)
    
    @Enumerated(EnumType.STRING)
    @Column
    private EventStatus status;
    
    @Column
    private String location;
    
    @Column
    private Boolean reminder;
    
    @Column
    private Integer reminderMinutes;
    
    public enum EventType {
        ASSIGNMENT,     // 과제
        EXAM,          // 시험
        CLASS,         // 수업
        MEETING,       // 회의
        PERSONAL,      // 개인 일정
        HOLIDAY        // 공휴일
    }
    
    public enum EventStatus {
        SCHEDULED,     // 예정
        IN_PROGRESS,   // 진행중
        COMPLETED,     // 완료
        CANCELLED      // 취소
    }
    
    public void updateStatus(EventStatus status) {
        this.status = status;
    }
    
    public void updateEvent(String title, String description, LocalDateTime startDateTime, 
                           LocalDateTime endDateTime, String color) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (startDateTime != null) this.startDateTime = startDateTime;
        if (endDateTime != null) this.endDateTime = endDateTime;
        if (color != null) this.color = color;
    }
}
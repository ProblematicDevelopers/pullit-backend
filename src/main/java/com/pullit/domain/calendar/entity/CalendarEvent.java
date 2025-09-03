package com.pullit.domain.calendar.entity;

import com.pullit.common.entity.BaseTimeEntity;
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
    
    // 일정 공개 범위 (개인/학급전체)
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    @Builder.Default
    private EventVisibility visibility = EventVisibility.PERSONAL;
    
    // 학급 ID (학급 전체 일정인 경우)
    @Column(name = "class_id")
    private Long classId;
    
    public enum EventType {
        ASSIGNMENT,     // 과제
        EXAM,          // 시험
        CBT_EXAM,      // CBT 시험
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
    
    public enum EventVisibility {
        PERSONAL,      // 개인 일정 (본인만 보기)
        CLASS_WIDE     // 학급 전체 일정 (학급 구성원 모두 보기)
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
package com.pullit.domain.calendar.dto;

import com.pullit.domain.calendar.entity.CalendarEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarEventResponse {
    
    private Long id;
    private String title;
    private String description;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private Boolean allDay;
    private Long userId;
    private CalendarEvent.EventType eventType;
    private String color;
    private Long relatedId;
    private CalendarEvent.EventStatus status;
    private String location;
    private Boolean reminder;
    private Integer reminderMinutes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static CalendarEventResponse from(CalendarEvent event) {
        return CalendarEventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .startDateTime(event.getStartDateTime())
                .endDateTime(event.getEndDateTime())
                .allDay(event.getAllDay())
                .userId(event.getUserId())
                .eventType(event.getEventType())
                .color(event.getColor())
                .relatedId(event.getRelatedId())
                .status(event.getStatus())
                .location(event.getLocation())
                .reminder(event.getReminder())
                .reminderMinutes(event.getReminderMinutes())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}
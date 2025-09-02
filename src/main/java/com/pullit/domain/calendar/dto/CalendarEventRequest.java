package com.pullit.domain.calendar.dto;

import com.pullit.domain.calendar.entity.CalendarEvent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarEventRequest {
    
    @NotBlank(message = "제목은 필수입니다")
    private String title;
    
    private String description;
    
    @NotNull(message = "시작 시간은 필수입니다")
    private LocalDateTime startDateTime;
    
    @NotNull(message = "종료 시간은 필수입니다")
    private LocalDateTime endDateTime;
    
    @Builder.Default
    private Boolean allDay = false;
    
    @NotNull(message = "사용자 ID는 필수입니다")
    private Long userId;
    
    @NotNull(message = "이벤트 타입은 필수입니다")
    private CalendarEvent.EventType eventType;
    
    private String color;
    
    private Long relatedId;
    
    private String location;
    
    @Builder.Default
    private Boolean reminder = false;
    
    private Integer reminderMinutes;
}
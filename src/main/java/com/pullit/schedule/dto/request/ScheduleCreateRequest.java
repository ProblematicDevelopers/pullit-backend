package com.pullit.schedule.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleCreateRequest {
    
    @NotBlank(message = "일정 제목은 필수입니다.")
    private String title;
    
    private String description;
    
    @NotBlank(message = "일정 유형은 필수입니다.")
    private String type; // "exam", "meeting", "deadline", "class", "event"
    
    @NotNull(message = "일정 날짜는 필수입니다.")
    private LocalDateTime scheduledDate;
    
    private LocalDateTime endDate;
    
    private String location;
    
    private Integer participants;
    
    private Long classId;
    
    private Long examId;
    
    private Boolean isRecurring;
    
    private String recurrencePattern; // "daily", "weekly", "monthly"
    
    private Boolean reminderEnabled;
    
    private Integer reminderMinutes;
    
    private String color; // 캘린더 표시 색상
}
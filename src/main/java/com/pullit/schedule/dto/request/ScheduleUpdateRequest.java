package com.pullit.schedule.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleUpdateRequest {
    
    private String title;
    
    private String description;
    
    private String type;
    
    private LocalDateTime scheduledDate;
    
    private LocalDateTime endDate;
    
    private String location;
    
    private Integer participants;
    
    private Boolean isRecurring;
    
    private String recurrencePattern;
    
    private Boolean reminderEnabled;
    
    private Integer reminderMinutes;
    
    private String color;
}
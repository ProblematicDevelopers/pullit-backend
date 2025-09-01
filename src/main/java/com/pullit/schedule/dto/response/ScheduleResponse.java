package com.pullit.schedule.dto.response;

import com.pullit.schedule.entity.Schedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponse {
    
    private Long id;
    private String title;
    private String description;
    private String type;
    private LocalDateTime scheduledDate;
    private LocalDateTime endDate;
    private String location;
    private Integer participants;
    private String status;
    private Long classId;
    private Long examId;
    private Boolean isRecurring;
    private String recurrencePattern;
    private Boolean reminderEnabled;
    private Integer reminderMinutes;
    private String color;
    private LocalDateTime createdDate;
    private Long createdBy;
    
    public static ScheduleResponse from(Schedule schedule) {
        return ScheduleResponse.builder()
                .id(schedule.getId())
                .title(schedule.getTitle())
                .description(schedule.getDescription())
                .type(schedule.getType())
                .scheduledDate(schedule.getScheduledDate())
                .endDate(schedule.getEndDate())
                .location(schedule.getLocation())
                .participants(schedule.getParticipants())
                .status(schedule.getStatus())
                .classId(schedule.getClassId())
                .examId(schedule.getExamId())
                .isRecurring(schedule.getIsRecurring())
                .recurrencePattern(schedule.getRecurrencePattern())
                .reminderEnabled(schedule.getReminderEnabled())
                .reminderMinutes(schedule.getReminderMinutes())
                .color(schedule.getColor())
                .createdDate(schedule.getCreatedDate())
                .createdBy(schedule.getCreatedBy())
                .build();
    }
}
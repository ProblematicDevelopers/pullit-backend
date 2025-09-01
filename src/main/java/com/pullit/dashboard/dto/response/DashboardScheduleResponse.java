package com.pullit.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardScheduleResponse {
    private Long id;
    private String title;
    private String type; // "exam", "meeting", "deadline", "class"
    private LocalDate date;
    private LocalTime time;
    private LocalDateTime scheduledDateTime;
    private String dateDisplay; // "내일", "금요일", "월요일", "다음주"
    private String timeDisplay; // "오전 10:00", "오후 2:00"
    private Integer participants; // 참여 학생 수
    private String description;
    private Long examId; // 시험인 경우 exam_id
    private String status; // "upcoming", "ongoing", "completed"
}
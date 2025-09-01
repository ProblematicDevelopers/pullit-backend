package com.pullit.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardActivityResponse {
    private Long id;
    private String type; // "exam_created", "exam_attempted", "student_joined", "grade_updated"
    private String title;
    private String description;
    private LocalDateTime activityTime;
    private String relativeTime; // "2시간 전", "어제", etc.
    private String iconType; // "exam", "student", "grade"
    private Long relatedId; // 관련 엔티티 ID (exam_id, student_id 등)
}
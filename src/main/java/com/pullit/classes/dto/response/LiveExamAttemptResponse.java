package com.pullit.classes.dto.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LiveExamAttemptResponse {
    private Long attemptId;
    private Long examId;
    private String status; // "IN_PROGRESS", "COMPLETED", "PENDING"
    private Long userId;
    private Integer remainTime;
    private String startTime;
    private String endTime;
}

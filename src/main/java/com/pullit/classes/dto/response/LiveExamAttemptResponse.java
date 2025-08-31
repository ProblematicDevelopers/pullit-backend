package com.pullit.classes.dto.response;

import com.pullit.cbt.entity.AttemptExam;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LiveExamAttemptResponse {
    private Long attemptId;
    private Long examId;
    private AttemptExam.AttemptStatus status; // "IN_PROGRESS", "COMPLETED", "PENDING"
    private Long userId;
    private Integer remainTime;
    private String startTime;
    private String endTime;
    private String message; // 상태 메시지 (예: "이미 완료된 시험입니다.")
}

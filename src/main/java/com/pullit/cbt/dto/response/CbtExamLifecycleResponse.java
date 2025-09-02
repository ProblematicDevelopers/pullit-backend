package com.pullit.cbt.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CbtExamLifecycleResponse {
    private Long examId;
    private String examName;
    private Long classId;
    private String className;
    private List<Long> studentIds;
    private String status; // STARTED, ENDED
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String message;
    private Integer notificationsSent; // 발송된 알림 수
}
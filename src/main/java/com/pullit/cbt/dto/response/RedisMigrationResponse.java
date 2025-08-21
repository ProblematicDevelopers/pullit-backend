package com.pullit.cbt.dto.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RedisMigrationResponse {
    private Long attemptId;
    private Boolean success;
    private String message;
    private Integer migratedQuestions;
    private Integer totalTime;
    private String completedAt;
}

package com.pullit.classes.dto.request;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LiveExamAttemptRequest {
    private Long examId;
    private Long classId;
}

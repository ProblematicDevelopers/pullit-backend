package com.pullit.cbt.dto.request;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CbtAttemptRequest {
    private Long examId;
}

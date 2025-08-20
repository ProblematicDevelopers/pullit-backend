package com.pullit.cbt.dto.response;

import lombok.*;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RedisDataResponse {
    private Long attemptId;
    private Map<String, Object> data;
    private Boolean success;
    private String message;
    private Integer totalKeys;
}

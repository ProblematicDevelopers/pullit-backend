package com.pullit.cbt.dto.request;

import lombok.*;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RedisUpdateRequest {
    private String key;
    private String value;
    private Long expiration; // 만료 시간 (초)
    private Map<String, Object> data; // 추가 데이터
}

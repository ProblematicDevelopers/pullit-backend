package com.pullit.cbt.dto.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RedisUpdateResponse {
    private String key;
    private String value;
    private Boolean success;
    private String message;
    private Long expiration;
}

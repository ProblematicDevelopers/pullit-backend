package com.pullit.websocket.dto.request;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OnlineStatusRequest {
    private String channelName; // 채널 이름
    private Long userId; // 사용자 ID (문자열로 변경)
    private String userName;
    private String userRole; // TEACHER, STUDENT
    private String status; // ONLINE, OFFLINE, AWAY
    private String timestamp;
}

package com.pullit.websocket.dto.request;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OnlineStatusRequest {
    private Long classId;
    private Long userId;
    private String userName;
    private String userRole; // TEACHER, STUDENT
    private String status; // ONLINE, OFFLINE, AWAY
    private String timestamp;
}

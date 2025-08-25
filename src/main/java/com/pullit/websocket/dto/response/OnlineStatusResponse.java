package com.pullit.websocket.dto.response;

import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OnlineStatusResponse {
    private Long classId;
    private List<UserOnlineStatus> onlineUsers;
    private String timestamp;
    private String status; // SUCCESS, ERROR
    private String errorMessage;
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserOnlineStatus {
        private Long userId;
        private String userName;
        private String userRole;
        private String status; // ONLINE, OFFLINE, AWAY
        private String lastSeen;
    }
}

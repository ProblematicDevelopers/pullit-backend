package com.pullit.websocket.service;

import com.pullit.websocket.dto.request.OnlineStatusRequest;
import com.pullit.websocket.dto.response.OnlineStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OnlineStatusService {

    // 클래스별 온라인 사용자 관리 (메모리 기반, 실제로는 Redis 사용 권장)
    private final Map<String, Map<String, UserOnlineInfo>> classOnlineUsers = new ConcurrentHashMap<>();

    public void updateUserStatus(OnlineStatusRequest request) {
        log.info("Updating user status: userId={}, classId={}, status={}", 
                request.getUserId(), request.getChannelName(), request.getStatus());
        
        // null 체크
        if (request == null || request.getChannelName() == null || request.getUserId() == null) {
            log.error("Invalid request data: request={}, classId={}, userId={}", 
                     request, request != null ? request.getChannelName() : null, 
                     request != null ? request.getUserId() : null);
            return;
        }
        
        String channelName = request.getChannelName();
        Long userId = request.getUserId();
        
        // 클래스별 사용자 맵 가져오기 (없으면 생성)
        Map<String, UserOnlineInfo> users = classOnlineUsers.computeIfAbsent(channelName, k -> new ConcurrentHashMap<>());
        
        // 사용자 상태 업데이트
        UserOnlineInfo userInfo = UserOnlineInfo.builder()
                .userId(userId)
                .userName(request.getUserName())
                .userRole(request.getUserRole())
                .status(request.getStatus())
                .lastSeen(LocalDateTime.now())
                .build();
        
        users.put(userId.toString(), userInfo);
        log.info("User status updated: {}", userInfo);
    }

    public void removeUserFromClass(String channelName, Long userId) {
        log.info("Removing user from class: userId={}, channelName={}", userId, channelName);
        
        // null 체크
        if (channelName == null || userId == null) {
            log.error("Invalid parameters for removeUserFromClass: channelName={}, userId={}", channelName, userId);
            return;
        }
        
        Map<String, UserOnlineInfo> users = null;
        try {
            users = classOnlineUsers.get(channelName);
        } catch (NullPointerException e) {
            log.error("NullPointerException when removing user from classId: {}", channelName, e);
            return;
        }
        
        if (users != null) {
            users.remove(userId.toString());
            
            // 클래스에 사용자가 없으면 클래스 맵도 제거
            if (users.isEmpty()) {
                classOnlineUsers.remove(channelName);
            }
        }
    }

    public OnlineStatusResponse getClassOnlineStatus(String channelName) {
        log.info("Getting online status for class: {}", channelName);
        
        // null 체크
        if (channelName == null) {
            log.error("ChannelName is null for getClassOnlineStatus");
            return OnlineStatusResponse.builder()
                    .channelName(null)
                    .onlineUsers(List.of())
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .status("ERROR")
                    .errorMessage("ClassId is null")
                    .build();
        }
        
        Map<String, UserOnlineInfo> users = null;
        try {
            users = classOnlineUsers.get(channelName);
        } catch (NullPointerException e) {
            log.error("NullPointerException when getting users for classId: {}", channelName, e);
            users = null;
        }
        List<OnlineStatusResponse.UserOnlineStatus> onlineUsers = null;
        
        if (users != null) {
            onlineUsers = users.values().stream()
                    .map(this::convertToUserOnlineStatus)
                    .collect(Collectors.toList());
        } else {
            onlineUsers = List.of();
        }
        
        return OnlineStatusResponse.builder()
                .channelName(channelName)
                .onlineUsers(onlineUsers)
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .status("SUCCESS")
                .build();
    }

    public int getOnlineUserCount(String channelName) {
        // null 체크
        if (channelName == null) {
            log.error("ChannelName is null for getOnlineUserCount");
            return 0;
        }
        
        Map<String, UserOnlineInfo> users = null;
        try {
            users = classOnlineUsers.get(channelName);
        } catch (NullPointerException e) {
            log.error("NullPointerException when getting users count for classId: {}", channelName, e);
            return 0;
        }
        return users != null ? users.size() : 0;
    }

    private OnlineStatusResponse.UserOnlineStatus convertToUserOnlineStatus(UserOnlineInfo userInfo) {
        return OnlineStatusResponse.UserOnlineStatus.builder()
                .userId(userInfo.getUserId())
                .userName(userInfo.getUserName())
                .userRole(userInfo.getUserRole())
                .status(userInfo.getStatus())
                .lastSeen(userInfo.getLastSeen().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    // 내부 클래스: 사용자 온라인 정보
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    private static class UserOnlineInfo {
        private Long userId;
        private String userName;
        private String userRole;
        private String status;
        private LocalDateTime lastSeen;
    }
}

package com.pullit.notification.websocket;

import com.pullit.notification.dto.response.NotificationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {
    
    private final ObjectMapper objectMapper;
    
    // userId -> WebSocket Sessions mapping
    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    
    // Session -> userId mapping
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = extractUserIdFromSession(session);
        
        if (userId != null) {
            // 사용자 세션 저장
            userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>())
                    .add(session);
            sessionUserMap.put(session.getId(), userId);
            
            log.info("WebSocket connection established for user: {}, session: {}", 
                    userId, session.getId());
            
            // 연결 성공 메시지 전송
            sendMessage(session, Map.of(
                    "type", "CONNECTION",
                    "message", "Connected to notification service",
                    "userId", userId
            ));
        } else {
            log.warn("WebSocket connection rejected - no user ID found");
            session.close(CloseStatus.NOT_ACCEPTABLE);
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        Long userId = sessionUserMap.remove(sessionId);
        
        if (userId != null) {
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
            
            log.info("WebSocket connection closed for user: {}, session: {}", 
                    userId, sessionId);
        }
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            
            String type = (String) data.get("type");
            
            switch (type) {
                case "PING":
                    handlePing(session);
                    break;
                case "MARK_READ":
                    handleMarkAsRead(session, data);
                    break;
                case "GET_UNREAD_COUNT":
                    handleGetUnreadCount(session);
                    break;
                default:
                    log.warn("Unknown message type: {}", type);
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket message: ", e);
            sendError(session, "Failed to process message");
        }
    }
    
    /**
     * 특정 사용자에게 알림 전송
     */
    public void sendNotificationToUser(Long userId, NotificationResponse notification) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        
        if (sessions != null && !sessions.isEmpty()) {
            Map<String, Object> message = Map.of(
                    "type", "NOTIFICATION",
                    "data", notification
            );
            
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        sendMessage(session, message);
                        log.debug("Notification sent to user {} via WebSocket", userId);
                    } catch (Exception e) {
                        log.error("Failed to send notification to session {}: ", 
                                session.getId(), e);
                    }
                }
            }
        } else {
            log.debug("No active WebSocket session for user {}", userId);
        }
    }
    
    /**
     * 읽지 않은 알림 개수 업데이트 전송
     */
    public void sendUnreadCountUpdate(Long userId, long count) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        
        if (sessions != null && !sessions.isEmpty()) {
            Map<String, Object> message = Map.of(
                    "type", "UNREAD_COUNT_UPDATE",
                    "count", count
            );
            
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        sendMessage(session, message);
                    } catch (Exception e) {
                        log.error("Failed to send unread count to session {}: ", 
                                session.getId(), e);
                    }
                }
            }
        }
    }
    
    /**
     * 모든 연결된 사용자에게 브로드캐스트
     */
    public void broadcast(Map<String, Object> message) {
        userSessions.values().stream()
                .flatMap(Set::stream)
                .filter(WebSocketSession::isOpen)
                .forEach(session -> {
                    try {
                        sendMessage(session, message);
                    } catch (Exception e) {
                        log.error("Failed to broadcast to session {}: ", 
                                session.getId(), e);
                    }
                });
    }
    
    // Private helper methods
    
    private Long extractUserIdFromSession(WebSocketSession session) {
        // 실제 구현에서는 JWT 토큰이나 세션에서 사용자 ID 추출
        // 여기서는 URI 쿼리 파라미터에서 추출하는 예시
        String query = session.getUri().getQuery();
        if (query != null && query.contains("userId=")) {
            try {
                String userIdStr = query.split("userId=")[1].split("&")[0];
                return Long.parseLong(userIdStr);
            } catch (Exception e) {
                log.error("Failed to extract user ID from query: {}", query, e);
            }
        }
        
        // Principal에서 추출 시도
        if (session.getPrincipal() != null) {
            try {
                return Long.parseLong(session.getPrincipal().getName());
            } catch (Exception e) {
                log.error("Failed to extract user ID from principal", e);
            }
        }
        
        return null;
    }
    
    private void handlePing(WebSocketSession session) throws IOException {
        sendMessage(session, Map.of("type", "PONG"));
    }
    
    private void handleMarkAsRead(WebSocketSession session, Map<String, Object> data) {
        // 알림 읽음 처리는 REST API를 통해 수행
        // 여기서는 확인 메시지만 전송
        try {
            sendMessage(session, Map.of(
                    "type", "MARK_READ_RESPONSE",
                    "success", true
            ));
        } catch (IOException e) {
            log.error("Failed to send mark read response", e);
        }
    }
    
    private void handleGetUnreadCount(WebSocketSession session) {
        // 읽지 않은 알림 개수는 REST API를 통해 조회
        // 여기서는 요청 확인만
        try {
            sendMessage(session, Map.of(
                    "type", "UNREAD_COUNT_REQUEST_RECEIVED"
            ));
        } catch (IOException e) {
            log.error("Failed to send unread count response", e);
        }
    }
    
    private void sendMessage(WebSocketSession session, Object message) throws IOException {
        if (session.isOpen()) {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        }
    }
    
    private void sendError(WebSocketSession session, String error) {
        try {
            sendMessage(session, Map.of(
                    "type", "ERROR",
                    "message", error
            ));
        } catch (IOException e) {
            log.error("Failed to send error message", e);
        }
    }
    
    /**
     * 현재 연결된 사용자 수 조회
     */
    public int getConnectedUsersCount() {
        return userSessions.size();
    }
    
    /**
     * 특정 사용자의 연결 상태 확인
     */
    public boolean isUserConnected(Long userId) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty() && 
               sessions.stream().anyMatch(WebSocketSession::isOpen);
    }
}
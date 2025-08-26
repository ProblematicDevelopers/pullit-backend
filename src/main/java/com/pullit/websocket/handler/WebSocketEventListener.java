package com.pullit.websocket.handler;

import com.pullit.websocket.service.OnlineStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final OnlineStatusService onlineStatusService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        log.info("Received a new web socket connection");
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // 세션에서 사용자 정보 가져오기
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        String channelName = (String) headerAccessor.getSessionAttributes().get("channelName");

        if (username != null && userId != null && channelName != null) {
            log.info("User Disconnected: {} (userId: {}, channelName: {})", username, userId, channelName);

            // 접속 상태에서 사용자 제거
            onlineStatusService.removeUserFromClass(channelName, userId);

            // 클래스 전체에 접속 상태 업데이트 브로드캐스트
            // (이 부분은 SimpMessagingTemplate이 필요하므로 별도 처리 필요)
        }
    }
}

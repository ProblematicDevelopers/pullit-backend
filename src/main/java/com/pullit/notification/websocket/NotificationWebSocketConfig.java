package com.pullit.notification.websocket;

import com.pullit.common.config.properties.SecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class NotificationWebSocketConfig implements WebSocketConfigurer {
    
    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final SecurityProperties securityProperties;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // SecurityProperties에서 CORS 설정을 가져와서 사용
        String[] allowedOrigins = securityProperties.getCors().getAllowedOrigins()
                .toArray(new String[0]);
        
        // 일반 WebSocket 연결 허용
        registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
                .setAllowedOrigins(allowedOrigins);
        
        // SockJS 폴백도 지원
        registry.addHandler(notificationWebSocketHandler, "/ws/notifications-sockjs")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();
    }
}
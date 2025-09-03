package com.pullit.websocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

/**
 * WebSocket 보안 설정
 * 
 * WebSocket 연결 자체는 HTTP 핸드셰이크를 통해 이루어지므로 SecurityConfig에서 permitAll()로 설정하고,
 * 실제 메시지 레벨의 보안은 이 설정에서 처리합니다.
 */
@Configuration
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
                // STOMP CONNECT 프레임은 인증 없이 허용 (JWT는 헤더로 전달)
                .nullDestMatcher().permitAll()
                
                // 구독 경로별 권한 설정
                .simpSubscribeDestMatchers("/user/queue/notifications").authenticated()
                .simpSubscribeDestMatchers("/topic/class/**").authenticated()
                .simpSubscribeDestMatchers("/topic/admin/**").hasAuthority("ROLE_ADMIN")
                .simpSubscribeDestMatchers("/topic/teacher/**").hasAuthority("ROLE_TEACHER")
                
                // 메시지 전송 경로별 권한 설정
                .simpDestMatchers("/app/chat/**").authenticated()
                .simpDestMatchers("/app/notification/**").authenticated()
                
                // 기타 모든 메시지는 인증 필요
                .anyMessage().authenticated();
    }

    @Override
    protected boolean sameOriginDisabled() {
        // CORS를 위해 same origin 정책 비활성화
        return true;
    }
}
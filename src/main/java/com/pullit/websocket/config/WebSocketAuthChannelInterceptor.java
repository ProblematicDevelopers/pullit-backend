package com.pullit.websocket.config;

import com.pullit.auth.converter.JwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Extracts JWT from STOMP CONNECT headers and authenticates the WebSocket session.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = resolveBearerToken(accessor);
            if (StringUtils.hasText(token)) {
                try {
                    Jwt jwt = jwtDecoder.decode(token);
                    AbstractAuthenticationToken authentication = jwtAuthenticationConverter.convert(jwt);
                    if (authentication != null) {
                        authentication.setAuthenticated(true);
                        accessor.setUser(authentication);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("WebSocket CONNECT authenticated for user: {}", authentication.getName());
                    }
                } catch (JwtException ex) {
                    log.warn("Invalid JWT on STOMP CONNECT: {}", ex.getMessage());
                }
            } else {
                log.debug("No Authorization header on STOMP CONNECT");
            }
        }

        return message;
    }

    private String resolveBearerToken(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders == null || authHeaders.isEmpty()) {
            authHeaders = accessor.getNativeHeader("authorization");
        }
        if (authHeaders == null || authHeaders.isEmpty()) {
            return null;
        }
        String value = authHeaders.get(0);
        if (StringUtils.hasText(value) && value.startsWith("Bearer ")) {
            return value.substring(7);
        }
        return null;
    }
}


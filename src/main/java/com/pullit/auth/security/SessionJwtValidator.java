package com.pullit.auth.security;

import com.pullit.auth.service.ActiveSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionJwtValidator implements OAuth2TokenValidator<Jwt> {

    private final ActiveSessionService activeSessionService;

    private static final OAuth2Error INVALID_SESSION_ERROR = new OAuth2Error(
            "invalid_session",
            "JWT sessionId does not match the active session",
            null
    );

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        try {
            Object sub = token.getClaims().get("sub");
            Object sid = token.getClaims().get("sessionId");

            // Backward compatibility: if no sessionId in token, accept it
            if (sid == null || sub == null) {
                return OAuth2TokenValidatorResult.success();
            }

            Long userId = Long.parseLong(sub.toString());
            String sessionId = sid.toString();

            boolean ok = activeSessionService.isActiveSession(userId, sessionId);
            if (ok) {
                return OAuth2TokenValidatorResult.success();
            } else {
                log.warn("Session validation failed for user {} (token sid={})", userId, sessionId);
                return OAuth2TokenValidatorResult.failure(INVALID_SESSION_ERROR);
            }
        } catch (Exception e) {
            log.error("Error during session validation", e);
            return OAuth2TokenValidatorResult.failure(INVALID_SESSION_ERROR);
        }
    }
}


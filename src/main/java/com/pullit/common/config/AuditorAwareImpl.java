package com.pullit.common.config;

import com.pullit.auth.authentication.CustomUserDetails;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;

/**
 * JPA Auditing을 위한 현재 사용자 정보 제공 구현체
 * createdBy, updatedBy 필드에 자동으로 사용자 ID를 설정
 * AuditingConfig에서 Bean으로 등록됨
 */
public class AuditorAwareImpl implements AuditorAware<Long> {
    
    @Override
    public Optional<Long> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // 인증 정보가 없거나 인증되지 않은 경우
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();  // 또는 시스템 사용자 ID (예: 0L)
        }
        
        // anonymousUser인 경우
        if (authentication.getPrincipal().equals("anonymousUser")) {
            return Optional.empty();
        }
        
        // CustomUserDetails 타입인 경우 userId 반환
        if (authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            return Optional.of(userDetails.getUserId());
        }
        
        // 기타 경우
        return Optional.empty();
    }
}
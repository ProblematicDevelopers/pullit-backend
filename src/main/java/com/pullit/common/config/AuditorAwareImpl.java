package com.pullit.common.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Optional;

/**
 * JPA Auditing을 위한 현재 사용자 정보 제공 구현체
 * createdBy, updatedBy 필드에 자동으로 사용자 정보를 설정
 * AuditingConfig에서 Bean으로 등록됨
 */
public class AuditorAwareImpl implements AuditorAware<String> {
    
    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // 인증 정보가 없거나 인증되지 않은 경우
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of("system");
        }
        
        // anonymousUser인 경우
        if (authentication.getPrincipal().equals("anonymousUser")) {
            return Optional.of("anonymous");
        }
        
        // UserDetails 타입인 경우 username 반환
        if (authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return Optional.ofNullable(userDetails.getUsername());
        }
        
        // 기타 경우 principal을 문자열로 변환
        return Optional.of(authentication.getPrincipal().toString());
    }
}
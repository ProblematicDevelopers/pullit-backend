package com.pullit.common.resolver;

import com.pullit.common.annotation.AuthUser;
import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @AuthUser 어노테이션이 있는 파라미터에 현재 인증된 사용자 정보 주입
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthUserArgumentResolver implements HandlerMethodArgumentResolver {
    
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthUser.class);
    }
    
    @Override
    public Object resolveArgument(MethodParameter parameter, 
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, 
                                  WebDataBinderFactory binderFactory) throws Exception {
        
        AuthUser authUser = parameter.getParameterAnnotation(AuthUser.class);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
            authentication.getPrincipal().equals("anonymousUser")) {
            
            if (authUser.required()) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "인증된 사용자 정보가 필요합니다");
            }
            return null;
        }
        
        // 실제 구현 시 UserDetails나 커스텀 User 객체 반환
        // 예: return (User) authentication.getPrincipal();
        return authentication.getPrincipal();
    }
}
package com.pullit.auth.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class OAuth2Config {

    private final com.pullit.common.config.properties.SecurityProperties securityProperties;

    @Bean
    @Order(1) // OAuth2 필터 체인이 먼저 실행되도록 설정
    public SecurityFilterChain oauth2FilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/oauth2/**", "/api/auth/oauth2/**", "/login/oauth2/code/**")
            .cors(cors -> cors.configurationSource(oauth2CorsConfigurationSource()))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/oauth2/**", "/api/auth/oauth2/**", "/login/oauth2/code/**").permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .csrf(csrf -> csrf.disable()); // OAuth2 콜백 처리 시 CSRF 비활성화
        
        return http.build();
    }

    @Bean
    public CorsConfigurationSource oauth2CorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        var corsProps = securityProperties.getCors();
        configuration.setAllowedOrigins(corsProps.getAllowedOrigins());
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(corsProps.isAllowCredentials());
        configuration.setMaxAge(corsProps.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

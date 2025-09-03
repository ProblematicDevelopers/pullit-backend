package com.pullit.common.config;

import com.pullit.auth.converter.JwtAuthenticationConverter;
import com.pullit.auth.security.JwtAccessDeniedHandler;
import com.pullit.auth.security.JwtAuthenticationEntryPoint;
import com.pullit.common.config.properties.SecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SecurityProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final SecurityProperties securityProperties;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                .authorizeHttpRequests(auth -> auth
                        // ===== PUBLIC ENDPOINTS (인증 불필요) =====
                        // Swagger 및 API 문서
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        
                        // WebSocket 엔드포인트 (WebSocket은 초기 핸드셰이크만 허용, 실제 인증은 WebSocket 레벨에서 처리)
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/ws/notifications/**").permitAll()
                        .requestMatchers("/sockjs-node/**").permitAll()
                        .requestMatchers("/stomp/**").permitAll()
                        
                        // 인증/인가 관련 엔드포인트
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/register").permitAll()
                        .requestMatchers("/api/auth/refresh").permitAll()
                        .requestMatchers("/api/auth/refresh-token").permitAll()
                        .requestMatchers("/api/auth/validate").permitAll()
                        .requestMatchers("/api/auth/oauth2/**").permitAll()
                        .requestMatchers("/api/oauth2/**").permitAll()
                        
                        // 사용자 중복 체크 (회원가입시 필요)
                        .requestMatchers("/api/users/check/**").permitAll()
                        
                        // 이메일/SMS 인증 관련
                        .requestMatchers("/api/verification/**").permitAll()
                        
                        // 이미지 프록시 (공개 이미지)
                        .requestMatchers("/api/images/proxy/**").permitAll()
                        
                        // ===== ADMIN ONLY ENDPOINTS =====
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/schools/manage/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/users/manage/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/system/**").hasAuthority("ROLE_ADMIN")
                        
                        // ===== TEACHER ENDPOINTS =====
                        // 교사 전용 통계 및 관리
                        .requestMatchers("/api/teacher/**").hasAuthority("ROLE_TEACHER")
                        .requestMatchers("/api/teacher-live-exams/**").hasAuthority("ROLE_TEACHER")
                        .requestMatchers("/api/classes/manage/**").hasAuthority("ROLE_TEACHER")
                        .requestMatchers("/api/assignments/create").hasAuthority("ROLE_TEACHER")
                        .requestMatchers("/api/assignments/*/edit").hasAuthority("ROLE_TEACHER")
                        .requestMatchers("/api/assignments/*/delete").hasAuthority("ROLE_TEACHER")
                        
                        // 교사가 학생 관리
                        .requestMatchers("/api/students/manage/**").hasAuthority("ROLE_TEACHER")
                        .requestMatchers("/api/classes/*/students/**").hasAuthority("ROLE_TEACHER")
                        
                        // 시험 생성 및 관리 (교사만)
                        .requestMatchers("/api/exams/create").hasAuthority("ROLE_TEACHER")
                        .requestMatchers("/api/exams/*/edit").hasAuthority("ROLE_TEACHER")
                        .requestMatchers("/api/exams/*/delete").hasAuthority("ROLE_TEACHER")
                        .requestMatchers("/api/exams/*/assign").hasAuthority("ROLE_TEACHER")
                        
                        // 문항 처리 및 OCR (교사만)
                        .requestMatchers("/api/item-process/**").hasAuthority("ROLE_TEACHER")
                        .requestMatchers("/api/ocr/**").hasAuthority("ROLE_TEACHER")
                        .requestMatchers("/api/file-history/**").hasAuthority("ROLE_TEACHER")
                        
                        // ===== STUDENT ENDPOINTS =====
                        // 학생 전용 엔드포인트
                        .requestMatchers("/api/students/me/**").hasAuthority("ROLE_STUDENT")
                        .requestMatchers("/api/submissions/submit").hasAuthority("ROLE_STUDENT")
                        .requestMatchers("/api/cbt/student/**").hasAuthority("ROLE_STUDENT")
                        
                        // ===== TEACHER OR STUDENT (교사 또는 학생) =====
                        .requestMatchers("/api/classes/**").hasAnyAuthority("ROLE_TEACHER", "ROLE_STUDENT")
                        .requestMatchers("/api/assignments/**").hasAnyAuthority("ROLE_TEACHER", "ROLE_STUDENT")
                        .requestMatchers("/api/submissions/**").hasAnyAuthority("ROLE_TEACHER", "ROLE_STUDENT")
                        .requestMatchers("/api/exams/**").hasAnyAuthority("ROLE_TEACHER", "ROLE_STUDENT")
                        .requestMatchers("/api/user-exams/**").hasAnyAuthority("ROLE_TEACHER", "ROLE_STUDENT")
                        .requestMatchers("/api/cbt/**").hasAnyAuthority("ROLE_TEACHER", "ROLE_STUDENT")
                        .requestMatchers("/api/reports/**").hasAnyAuthority("ROLE_TEACHER", "ROLE_STUDENT")
                        .requestMatchers("/api/stats/**").hasAnyAuthority("ROLE_TEACHER", "ROLE_STUDENT")
                        
                        // ===== AUTHENTICATED USERS (로그인한 모든 사용자) =====
                        // 사용자 프로필 관련
                        .requestMatchers("/api/users/me").authenticated()
                        .requestMatchers("/api/users/*/profile").authenticated()
                        .requestMatchers("/api/teachers/me").authenticated()
                        
                        // 일반 조회 기능
                        .requestMatchers("/api/items/**").authenticated()
                        .requestMatchers("/api/chapters/**").authenticated()
                        .requestMatchers("/api/subjects/**").authenticated()
                        .requestMatchers("/api/schools/**").authenticated()
                        .requestMatchers("/api/calendar/**").authenticated()
                        .requestMatchers("/api/schedule/**").authenticated()
                        .requestMatchers("/api/notifications/**").authenticated()
                        .requestMatchers("/api/dashboard/**").authenticated()
                        
                        // 파일 업로드/다운로드
                        .requestMatchers("/api/files/**").authenticated()
                        .requestMatchers("/api/images/**").authenticated()
                        
                        // 로그아웃 (인증 필요)
                        .requestMatchers("/api/auth/logout").authenticated()
                        
                        // 기타 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                .oauth2ResourceServer(oauth2->oauth2
                        .jwt(jwt->jwt.decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                       )
                ;
                
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        SecurityProperties.Cors corsProperties = securityProperties.getCors();
        
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setAllowCredentials(corsProperties.isAllowCredentials());
        configuration.setMaxAge(corsProperties.getMaxAge());
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
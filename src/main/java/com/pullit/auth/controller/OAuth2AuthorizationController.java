package com.pullit.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Controller
@RequiredArgsConstructor
public class OAuth2AuthorizationController {

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverClientId;
    
    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;



    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @GetMapping("/oauth2/authorization/naver")
    public String startNaverOAuth2() {
        try {
            // state 생성 (보안을 위한 랜덤 문자열)
            String state = generateState();
            
            // 네이버 OAuth2 인증 URL 생성
            String redirectUri = "http://localhost:8080/login/oauth2/code/naver";
            String scope = "name email";
            
            String authUrl = String.format(
                "https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id=%s&redirect_uri=%s&state=%s&scope=%s",
                naverClientId,
                URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
                state,
                URLEncoder.encode(scope, StandardCharsets.UTF_8)
            );
            
            log.info("Redirecting to Naver OAuth2: {}", authUrl);
            return "redirect:" + authUrl;
            
        } catch (Exception e) {
            log.error("Error starting Naver OAuth2", e);
            return "redirect:http://localhost:5173/login?error=oauth2_start_failed";
        }
    }



    @GetMapping("/oauth2/authorization/kakao")
    public String startKakaoOAuth2() {
        try {
            // state 생성
            String state = generateState();
            
            // 카카오 OAuth2 인증 URL 생성
            String redirectUri = "http://localhost:8080/login/oauth2/code/kakao";
            String scope = "profile_nickname,account_email";
            
            String authUrl = String.format(
                "https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=%s&redirect_uri=%s&state=%s&scope=%s",
                kakaoClientId,
                URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
                state,
                URLEncoder.encode(scope, StandardCharsets.UTF_8)
            );
            
            log.info("Redirecting to Kakao OAuth2: {}", authUrl);
            return "redirect:" + authUrl;
            
        } catch (Exception e) {
            log.error("Error starting Kakao OAuth2", e);
            return "redirect:http://localhost:5173/login?error=oauth2_start_failed";
        }
    }

    @GetMapping("/oauth2/authorization/google")
    public String startGoogleOAuth2() {
        try {
            // state 생성
            String state = generateState();
            
            // 구글 OAuth2 인증 URL 생성
            String redirectUri = "http://localhost:8080/login/oauth2/code/google";
            String scope = "email profile";
            
            String authUrl = String.format(
                "https://accounts.google.com/o/oauth2/v2/auth?response_type=code&client_id=%s&redirect_uri=%s&state=%s&scope=%s",
                googleClientId,
                URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
                state,
                URLEncoder.encode(scope, StandardCharsets.UTF_8)
            );
            
            log.info("Redirecting to Google OAuth2: {}", authUrl);
            return "redirect:" + authUrl;
            
        } catch (Exception e) {
            log.error("Error starting Google OAuth2", e);
            return "redirect:http://localhost:5173/login?error=oauth2_start_failed";
        }
    }

    private String generateState() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

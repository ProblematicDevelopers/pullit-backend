package com.pullit.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class OAuth2CallbackController {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverClientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverClientSecret;
    
    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;
    
    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String kakaoClientSecret;
    


    @GetMapping("/login/oauth2/code/naver")
    public String handleNaverCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletRequest request) {
        
        try {
            log.info("Naver OAuth2 callback received - code: {}, state: {}", code, state);
            
            // 1. 액세스 토큰 요청
            String tokenUrl = "https://nid.naver.com/oauth2.0/token";
            MultiValueMap<String, String> tokenParams = new LinkedMultiValueMap<>();
            tokenParams.add("grant_type", "authorization_code");
            tokenParams.add("client_id", naverClientId);
            tokenParams.add("client_secret", naverClientSecret);
            tokenParams.add("code", code);
            tokenParams.add("state", state);
            
            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(tokenUrl, tokenParams, Map.class);
            Map<String, Object> tokenData = tokenResponse.getBody();
            
            if (tokenData != null && tokenData.containsKey("access_token")) {
                String accessToken = (String) tokenData.get("access_token");
                log.info("Access token received: {}", accessToken);
                
                // 2. 사용자 정보 요청
                String userInfoUrl = "https://openapi.naver.com/v1/nid/me";
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + accessToken);
                
                ResponseEntity<Map> userInfoResponse = restTemplate.exchange(
                    userInfoUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
                
                Map<String, Object> userInfo = userInfoResponse.getBody();
                log.info("User info received: {}", userInfo);
                
                if (userInfo != null && userInfo.containsKey("response")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> response = (Map<String, Object>) userInfo.get("response");
                    
                    // 3. 소셜 정보를 세션에 저장
                    HttpSession session = request.getSession();
                    Map<String, Object> socialInfo = new HashMap<>();
                    socialInfo.put("provider", "naver");
                    socialInfo.put("providerId", response.get("id"));
                    socialInfo.put("email", response.get("email"));
                    socialInfo.put("name", response.get("name"));
                    
                    // 이메일에서 아이디 생성 (@ 앞부분 추출)
                    String email = (String) response.get("email");
                    String username = email != null ? email.split("@")[0] : "naver_" + response.get("id");
                    socialInfo.put("username", username);
                    
                    session.setAttribute("oauth2_social_info", socialInfo);
                    log.info("OAuth2 social info saved to session: {}", socialInfo);
                    log.info("Generated username from email: {} -> {}", email, username);
                    
                                                    // 4. 프론트엔드로 리다이렉트 (신규 사용자로 가정)
                                // 실제로는 여기서 사용자 존재 여부를 확인해야 함
                                return "redirect:http://localhost:5173/oauth2/callback/naver";
                } else {
                    log.warn("User info response is invalid: {}", userInfo);
                    return "redirect:http://localhost:5173/login?error=oauth2_userinfo_failed";
                }
            } else {
                log.warn("Token response is invalid: {}", tokenData);
                return "redirect:http://localhost:5173/login?error=oauth2_token_failed";
            }
            
        } catch (Exception e) {
            log.error("Error handling Naver OAuth2 callback", e);
            return "redirect:http://localhost:5173/login?error=oauth2_callback_failed";
        }
    }

    @GetMapping("/login/oauth2/code/kakao")
    public String handleKakaoCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletRequest request) {
        
        try {
            log.info("Kakao OAuth2 callback received - code: {}, state: {}", code, state);
            
            // 1. 액세스 토큰 요청
            String tokenUrl = "https://kauth.kakao.com/oauth/token";
            MultiValueMap<String, String> tokenParams = new LinkedMultiValueMap<>();
            tokenParams.add("grant_type", "authorization_code");
            tokenParams.add("client_id", kakaoClientId);
            tokenParams.add("client_secret", kakaoClientSecret);
            tokenParams.add("code", code);
            tokenParams.add("state", state);
            
            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(tokenUrl, tokenParams, Map.class);
            Map<String, Object> tokenData = tokenResponse.getBody();
            
            if (tokenData != null && tokenData.containsKey("access_token")) {
                String accessToken = (String) tokenData.get("access_token");
                log.info("Kakao access token received: {}", accessToken);
                
                // 2. 사용자 정보 요청
                String userInfoUrl = "https://kapi.kakao.com/v2/user/me";
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + accessToken);
                
                ResponseEntity<Map> userInfoResponse = restTemplate.exchange(
                    userInfoUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
                
                Map<String, Object> userInfo = userInfoResponse.getBody();
                log.info("Kakao user info received: {}", userInfo);
                
                if (userInfo != null && userInfo.containsKey("id")) {
                    // 3. 소셜 정보를 세션에 저장
                    HttpSession session = request.getSession();
                    Map<String, Object> socialInfo = new HashMap<>();
                    socialInfo.put("provider", "kakao");
                    socialInfo.put("providerId", userInfo.get("id").toString());
                    
                    // 카카오 계정 정보에서 이메일과 이름 추출
                    @SuppressWarnings("unchecked")
                    Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");
                    if (kakaoAccount != null) {
                        String email = (String) kakaoAccount.get("email");
                        socialInfo.put("email", email);
                        
                        @SuppressWarnings("unchecked")
                        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                        if (profile != null) {
                            String name = (String) profile.get("nickname");
                            socialInfo.put("name", name);
                        }
                        
                        // 이메일에서 아이디 생성 (@ 앞부분 추출)
                        String username = email != null ? email.split("@")[0] : "kakao_" + userInfo.get("id");
                        socialInfo.put("username", username);
                        
                        session.setAttribute("oauth2_social_info", socialInfo);
                        log.info("Kakao OAuth2 social info saved to session: {}", socialInfo);
                        log.info("Generated username from email: {} -> {}", email, username);
                        
                        // 4. 백엔드 내부 OAuth2 처리로 리다이렉트 (네이버와 동일한 흐름)
                        return "redirect:http://localhost:8080/api/auth/oauth2/success";
                    } else {
                        log.warn("Kakao account info not found");
                        return "redirect:http://localhost:5173/login?error=oauth2_userinfo_failed";
                    }
                } else {
                    log.warn("Kakao user info response is invalid: {}", userInfo);
                    return "redirect:http://localhost:5173/login?error=oauth2_userinfo_failed";
                }
            } else {
                log.warn("Kakao token response is invalid: {}", tokenData);
                return "redirect:http://localhost:5173/login?error=oauth2_token_failed";
            }
            
        } catch (Exception e) {
            log.error("Error handling Kakao OAuth2 callback", e);
            return "redirect:http://localhost:5173/login?error=oauth2_callback_failed";
        }
    }

    @GetMapping("/login/oauth2/code/google")
    public String handleGoogleCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletRequest request) {
        
        try {
            log.info("Google OAuth2 callback received - code: {}, state: {}", code, state);
            
            // Google OAuth2 처리 로직 (필요시 구현)
            return "redirect:http://localhost:5173/oauth2/callback/google";
            
        } catch (Exception e) {
            log.error("Error handling Google OAuth2 callback", e);
            return "redirect:http://localhost:5173/login?error=oauth2_callback_failed";
        }
    }


}

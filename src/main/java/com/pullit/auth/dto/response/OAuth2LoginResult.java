package com.pullit.auth.dto.response;

import com.pullit.user.dto.response.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2LoginResult {
    private String accessToken;
    private String refreshToken;
    private String provider;
    private String providerId;
    private String email;
    private String name;
    private String username;
    private UserResponse user;  // 사용자 정보 추가
}



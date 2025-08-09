package com.pullit.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {
    
    private Cors cors = new Cors();
    private Jwt jwt = new Jwt();
    private OAuth2 oauth2 = new OAuth2();
    
    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:5173", "http://localhost:3000");
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
        private List<String> allowedHeaders = List.of("*");
        private boolean allowCredentials = true;
        private long maxAge = 3600L;
    }
    
    @Getter
    @Setter
    public static class Jwt {
        private String privateKey;
        private String publicKey;
        private long accessTokenExpiration = 1800000; // 30분
        private long refreshTokenExpiration = 604800000; // 7일
        private String issuer = "pullit";
    }
    
    @Getter
    @Setter
    public static class OAuth2 {
        private List<String> authorizedRedirectUris = List.of(
            "http://localhost:5173/oauth2/redirect",
            "http://localhost:3000/oauth2/redirect"
        );
    }
}
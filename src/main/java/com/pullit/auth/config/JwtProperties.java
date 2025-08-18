package com.pullit.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    private String issuer = "pullit";

    private Long accessTokenExpiration = 86400L;

    private Long refreshTokenExpiration = 604800L;

    private String privateKey;
}

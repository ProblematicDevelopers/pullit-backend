package com.pullit.infrastructure.sms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "sms.coolsms")
@Getter
@Setter
public class SmsConfig {
    private String apiKey;
    private String apiSecret;
    private String from;
    private String apiUrl = "https://api.coolsms.co.kr";
    
    // 인증번호 관련 설정
    private int verificationCodeLength = 6;
    private long verificationCodeTtl = 180; // 3분 (초 단위)
}
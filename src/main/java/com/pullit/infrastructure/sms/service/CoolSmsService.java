package com.pullit.infrastructure.sms.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pullit.infrastructure.sms.config.SmsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.ContentType;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoolSmsService {
    
    private final SmsConfig smsConfig;
    private final Gson gson = new Gson();
    
    /**
     * SMS 발송
     * @param to 수신 번호
     * @param text 메시지 내용
     * @return 발송 성공 여부
     */
    public boolean sendSms(String to, String text) {
        try {
            // 인증 정보 생성
            String salt = UUID.randomUUID().toString().replaceAll("-", "");
            String date = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).toString().split("\\[")[0];
            String signature = generateSignature(date, salt);
            
            // 메시지 데이터 구성 - CoolSMS v4 API 형식
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("message", Map.of(
                "to", to.replaceAll("-", ""),
                "from", smsConfig.getFrom().replaceAll("-", ""),
                "text", text,
                "type", "SMS",
                "autoTypeDetect", false
            ));
            
            // HTTP 요청 생성
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost request = new HttpPost(smsConfig.getApiUrl() + "/messages/v4/send");
                
                // 헤더 설정
                request.setHeader("Authorization", "HMAC-SHA256 apiKey=" + smsConfig.getApiKey() + 
                                 ", date=" + date + ", salt=" + salt + ", signature=" + signature);
                request.setHeader("Content-Type", "application/json");
                
                // 요청 본문 설정
                String jsonBody = gson.toJson(requestBody);
                request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
                
                log.info("Sending SMS to: {}", to);
                log.debug("Request Body: {}", jsonBody);
                log.debug("Authorization Header: HMAC-SHA256 apiKey={}, date={}, salt={}, signature={}", 
                        smsConfig.getApiKey(), date, salt, signature);
                
                // 요청 실행 및 응답 로깅
                var response = httpClient.execute(request, httpResponse -> {
                    int statusCode = httpResponse.getCode();
                    String responseBody = new String(httpResponse.getEntity().getContent().readAllBytes());
                    log.info("SMS API Response Code: {}", statusCode);
                    log.debug("SMS API Response Body: {}", responseBody);
                    
                    if (statusCode >= 200 && statusCode < 300) {
                        return true;
                    } else {
                        log.error("SMS API Error Response: {}", responseBody);
                        return false;
                    }
                });
                
                if (response) {
                    log.info("SMS sent successfully to: {}", to);
                    return true;
                } else {
                    log.error("Failed to send SMS to: {}", to);
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Error sending SMS to {}: {}", to, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 인증번호 SMS 발송
     * @param to 수신 번호
     * @param verificationCode 인증번호
     * @return 발송 성공 여부
     */
    public boolean sendVerificationCode(String to, String verificationCode) {
        String message = String.format("[Pullit] 인증번호는 [%s]입니다. %d분 이내에 입력해주세요.", 
                                      verificationCode, 
                                      smsConfig.getVerificationCodeTtl() / 60);
        return sendSms(to, message);
    }
    
    /**
     * HMAC-SHA256 서명 생성
     */
    private String generateSignature(String date, String salt) throws Exception {
        String data = date + salt;
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(
            smsConfig.getApiSecret().getBytes(StandardCharsets.UTF_8), 
            "HmacSHA256"
        );
        sha256_HMAC.init(secret_key);
        return Hex.encodeHexString(sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}
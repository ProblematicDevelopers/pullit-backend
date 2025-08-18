package com.pullit.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Configuration
public class PropertyConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.public-key-location:}")
    private Resource publicKeyResource;

    @Value("${jwt.private-key-location:}")
    private Resource privateKeyResource;
    
    // 프로덕션 환경에서 사용할 환경변수
    @Value("${JWT_PUBLIC_KEY:}")
    private String publicKeyString;
    
    @Value("${JWT_PRIVATE_KEY:}")
    private String privateKeyString;

    /**
     * RSA 공개키 빈 등록 - 개발/로컬 환경
     */
    @Bean
    @Profile({"local", "dev", "default"})
    public RSAPublicKey publicKey() throws Exception {
        String key = new String(publicKeyResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        String publicKeyPEM = key
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(publicKeyPEM);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        log.info("RSA public key loaded from file: {}", publicKeyResource.getFilename());
        return (RSAPublicKey) keyFactory.generatePublic(spec);
    }
    
    /**
     * RSA 공개키 빈 등록 - 프로덕션 환경
     */
    @Bean
    @Profile("prod")
    public RSAPublicKey publicKeyProd() throws Exception {
        if (publicKeyString == null || publicKeyString.isEmpty()) {
            throw new IllegalStateException("JWT_PUBLIC_KEY environment variable is not set");
        }
        
        String publicKeyPEM = publicKeyString
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(publicKeyPEM);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        log.info("RSA public key loaded from environment variable");
        return (RSAPublicKey) keyFactory.generatePublic(spec);
    }

    /**
     * RSA 비밀키 빈 등록 - 개발/로컬 환경
     */
    @Bean
    @Profile({"local", "dev", "default"})
    public RSAPrivateKey privateKey() throws Exception {
        String key = new String(privateKeyResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        String privateKeyPEM = key
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(privateKeyPEM);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        log.info("RSA private key loaded from file: {}", privateKeyResource.getFilename());
        return (RSAPrivateKey) keyFactory.generatePrivate(spec);
    }
    
    /**
     * RSA 비밀키 빈 등록 - 프로덕션 환경
     */
    @Bean
    @Profile("prod")
    public RSAPrivateKey privateKeyProd() throws Exception {
        if (privateKeyString == null || privateKeyString.isEmpty()) {
            throw new IllegalStateException("JWT_PRIVATE_KEY environment variable is not set");
        }
        
        String privateKeyPEM = privateKeyString
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(privateKeyPEM);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        log.info("RSA private key loaded from environment variable");
        return (RSAPrivateKey) keyFactory.generatePrivate(spec);
    }
}

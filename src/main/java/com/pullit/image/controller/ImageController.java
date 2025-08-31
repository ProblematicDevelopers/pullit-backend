package com.pullit.image.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/image")
public class ImageController {

    @GetMapping("/proxy")
    public ResponseEntity<byte[]> proxyImage(@RequestParam String url) {
        try {
            log.info("이미지 프록시 요청: {}", url);
            
            // URL 디코딩
            String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8);
            log.info("디코딩된 URL: {}", decodedUrl);
            
            // HTTP 연결 생성
            URL imageUrl = new URL(decodedUrl);
            HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000); // 10초
            connection.setReadTimeout(10000); // 10초
            
            // User-Agent 설정 (일부 서버에서 요구)
            connection.setRequestProperty("User-Agent", 
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            int responseCode = connection.getResponseCode();
            log.info("응답 코드: {}", responseCode);
            
            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.error("이미지 로드 실패. 응답 코드: {}", responseCode);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
            }
            
            // 응답 헤더에서 Content-Type 가져오기
            String contentType = connection.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                contentType = "image/jpeg"; // 기본값
            }
            
            // 이미지 데이터 읽기
            try (InputStream inputStream = connection.getInputStream();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                
                byte[] imageData = outputStream.toByteArray();
                log.info("이미지 데이터 크기: {} bytes", imageData.length);
                
                // 응답 헤더 설정
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType(contentType));
                headers.setContentLength(imageData.length);
                headers.setCacheControl("max-age=3600"); // 1시간 캐시
                
                return new ResponseEntity<>(imageData, headers, HttpStatus.OK);
                
            } finally {
                connection.disconnect();
            }
            
        } catch (IOException e) {
            log.error("이미지 프록시 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("예상치 못한 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
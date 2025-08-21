package com.pullit.common.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

@RestController
@RequestMapping("/api/proxy")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:3000"})
public class ImageProxyController {

    @PostMapping("/image")
    public ResponseEntity<?> proxyImage(@RequestBody Map<String, String> request) {
        try {
            String imageUrl = request.get("imageUrl");
            
            if (imageUrl == null || imageUrl.isEmpty()) {
                return ResponseEntity.badRequest().body("Image URL is required");
            }

            // URL로부터 이미지 데이터 가져오기
            URL url = new URL(imageUrl);
            URLConnection connection = url.openConnection();
            
            // User-Agent 헤더 추가 (일부 서버는 이를 요구함)
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            // 이미지 데이터 읽기
            try (InputStream inputStream = connection.getInputStream();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                
                byte[] imageBytes = outputStream.toByteArray();
                
                // Content-Type 확인
                String contentType = connection.getContentType();
                if (contentType == null) {
                    // 확장자로 MIME 타입 추정
                    if (imageUrl.toLowerCase().endsWith(".svg")) {
                        contentType = "image/svg+xml";
                    } else if (imageUrl.toLowerCase().endsWith(".png")) {
                        contentType = "image/png";
                    } else if (imageUrl.toLowerCase().endsWith(".jpg") || imageUrl.toLowerCase().endsWith(".jpeg")) {
                        contentType = "image/jpeg";
                    } else {
                        contentType = "image/jpeg"; // 기본값
                    }
                }
                
                // SVG나 PNG를 JPEG로 변환
                byte[] jpegBytes = imageBytes;
                if (contentType.contains("svg") || contentType.contains("png") || !contentType.contains("jpeg")) {
                    try {
                        // 이미지를 BufferedImage로 읽기
                        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
                        
                        if (originalImage != null) {
                            // JPEG로 변환 (배경을 흰색으로)
                            BufferedImage jpegImage = new BufferedImage(
                                originalImage.getWidth(),
                                originalImage.getHeight(),
                                BufferedImage.TYPE_INT_RGB
                            );
                            
                            Graphics2D g = jpegImage.createGraphics();
                            g.setColor(Color.WHITE);
                            g.fillRect(0, 0, jpegImage.getWidth(), jpegImage.getHeight());
                            g.drawImage(originalImage, 0, 0, null);
                            g.dispose();
                            
                            // JPEG로 출력
                            ByteArrayOutputStream jpegOutputStream = new ByteArrayOutputStream();
                            ImageIO.write(jpegImage, "JPEG", jpegOutputStream);
                            jpegBytes = jpegOutputStream.toByteArray();
                            contentType = "image/jpeg";
                        }
                    } catch (Exception e) {
                        // 변환 실패 시 원본 사용
                        System.err.println("이미지 변환 실패: " + e.getMessage());
                    }
                }
                
                // Base64로 인코딩
                String base64Image = Base64.getEncoder().encodeToString(jpegBytes);
                String dataUrl = "data:" + contentType + ";base64," + base64Image;
                
                Map<String, String> response = new HashMap<>();
                response.put("base64", dataUrl);
                response.put("contentType", contentType);
                
                return ResponseEntity.ok(response);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch image: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
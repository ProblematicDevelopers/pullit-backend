package com.pullit.test.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Tag(name = "Test API", description = "API 연동 테스트용 엔드포인트")
@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @Operation(summary = "헬스 체크", description = "서버 상태를 확인합니다")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.info("Health check called");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Backend server is runningaaaaaabbbbb");
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "에코 테스트", description = "요청한 데이터를 그대로 반환합니다")
    @PostMapping("/echo")
    public ResponseEntity<Map<String, Object>> echo(@RequestBody Map<String, Object> request) {
        log.info("Echo endpoint called with data: {}", request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("received", request);
        response.put("message", "Echo successful");
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "CORS 테스트", description = "CORS 설정이 올바른지 확인합니다")
    @GetMapping("/cors-test")
    public ResponseEntity<Map<String, Object>> corsTest() {
        log.info("CORS test endpoint called");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "안녕~~");
        response.put("backend", "Spring Boot");
        response.put("frontend", "Vue.js");
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
}
package com.pullit.common.aspect;

import com.pullit.common.annotation.RateLimited;
import com.pullit.common.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitingAspect {

    private final RedisTemplate<String, String> redisTemplate;

    @Around("@annotation(rateLimited)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        String key = generateKey(joinPoint, rateLimited);
        String countKey = "rate_limit:" + key;

        // 현재 요청 횟수 증가
        Long currentCount = redisTemplate.opsForValue().increment(countKey);

        if (currentCount == null) {
            currentCount = 1L;
        }

        if (currentCount == 1) {
            // 첫 요청이면 만료 시간 설정
            redisTemplate.expire(countKey, rateLimited.duration(), rateLimited.timeUnit());
        }

        // 제한 초과 확인
        if (currentCount > rateLimited.limit()) {
            // 남은 시간 계산
            Long ttl = redisTemplate.getExpire(countKey, TimeUnit.SECONDS);
            String message = String.format(
                    "요청 한도(%d회)를 초과했습니다. %d초 후에 다시 시도해주세요.",
                    rateLimited.limit(),
                    ttl != null ? ttl : 60
            );

            log.warn("Rate limit exceeded for key: {} - count: {}/{}",
                    key, currentCount, rateLimited.limit());

            throw new RateLimitExceededException(message);
        }

        // 남은 요청 횟수를 응답 헤더에 추가 (선택사항)
        addRateLimitHeaders(rateLimited.limit(), currentCount);

        return joinPoint.proceed();
    }

    /**
     * Rate Limit 정보를 응답 헤더에 추가
     */
    private void addRateLimitHeaders(int limit, Long currentCount) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null && attributes.getResponse() != null) {
            var response = attributes.getResponse();
            response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            response.setHeader("X-RateLimit-Remaining",
                    String.valueOf(Math.max(0, limit - currentCount)));
            response.setHeader("X-RateLimit-Used", String.valueOf(currentCount));
        }
    }

    /**
     * Rate Limit 키 생성
     * 사용자별 또는 IP별로 구분
     */
    private String generateKey(ProceedingJoinPoint joinPoint, RateLimited rateLimited) {
        // 커스텀 키가 있으면 사용
        if (!rateLimited.key().isEmpty()) {
            return rateLimited.key();
        }

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return "unknown:" + joinPoint.getSignature().toShortString();
        }

        HttpServletRequest request = attributes.getRequest();

        // 인증된 사용자면 사용자 ID 사용
        if (request.getUserPrincipal() != null) {
            return "user:" + request.getUserPrincipal().getName() +
                    ":" + joinPoint.getSignature().toShortString();
        }

        // 아니면 IP 주소 사용
        String ip = getClientIP(request);
        return "ip:" + ip + ":" + joinPoint.getSignature().toShortString();
    }

    /**
     * 클라이언트 실제 IP 추출 (프록시 고려)
     */
    private String getClientIP(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "X-Real-IP"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 첫 번째 IP 주소 반환 (쉼표로 구분된 경우)
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }
}

package com.pullit.common.http.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.pullit.common.constants.HttpConstants.*;
@Slf4j
@Configuration
public class RestTemplateConfig {

    /**
     * 커넥션 풀 설정
     * HttpConstants의 연결 설정 사용
     */
    @Bean
    public PoolingHttpClientConnectionManager connectionManager() {
        PoolingHttpClientConnectionManager manager =
                new PoolingHttpClientConnectionManager();

        // HttpConstants의 연결 수 설정 사용
        manager.setMaxTotal(MAX_TOTAL_CONNECTIONS);  // 100
        manager.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);  // 20

        // 유휴 연결 검증 주기
        manager.setValidateAfterInactivity(TimeValue.ofSeconds(10));

        return manager;
    }

    /**
     * HttpClient 설정
     * Apache HttpClient5 사용
     */
    @Bean
    public HttpComponentsClientHttpRequestFactory httpRequestFactory(
            PoolingHttpClientConnectionManager connectionManager) {

        var httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                // HttpConstants의 유휴 연결 타임아웃 사용
                .evictIdleConnections(TimeValue.ofSeconds(IDLE_CONNECTION_TIMEOUT_SECONDS))  // 30초
                .evictExpiredConnections()  // 만료된 연결 제거
                .build();

        var requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        // HttpConstants의 타임아웃 설정 사용
        requestFactory.setConnectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS));  // 5초
        requestFactory.setConnectionRequestTimeout(Duration.ofMillis(CONNECTION_REQUEST_TIMEOUT_MS));  // 3초

        return requestFactory;
    }

    /**
     * 기본 RestTemplate
     * 로깅, 재시도, 에러 처리 인터셉터 포함
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder,
                                     HttpComponentsClientHttpRequestFactory requestFactory) {

        RestTemplate restTemplate = builder
                // 요청/응답 본문 재사용 가능하도록 버퍼링
                .requestFactory(() -> new BufferingClientHttpRequestFactory(requestFactory)).connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS)).readTimeout(Duration.ofMillis(READ_TIMEOUT_MS))  // 30초
                .build();

        // UTF-8 메시지 컨버터 추가
        restTemplate.getMessageConverters().add(0,
                new StringHttpMessageConverter(StandardCharsets.UTF_8));

        // 인터셉터 체인 구성
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(new LoggingInterceptor());     // 로깅
        interceptors.add(new RetryInterceptor());       // 재시도
        interceptors.add(new HeaderInterceptor());      // 공통 헤더
        restTemplate.setInterceptors(interceptors);

        // 커스텀 에러 핸들러 설정
        restTemplate.setErrorHandler(new CustomResponseErrorHandler());

        return restTemplate;
    }

    /**
     * 로깅 인터셉터
     * 요청/응답 로깅
     */
    @Slf4j
    public static class LoggingInterceptor implements ClientHttpRequestInterceptor {

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {

            // HttpConstants의 로그 메시지 사용
            log.debug(LOG_REQUEST, request.getMethod(), request.getURI());
            log.debug("요청 헤더: {}", request.getHeaders());

            long startTime = System.currentTimeMillis();
            ClientHttpResponse response = execution.execute(request, body);
            long duration = System.currentTimeMillis() - startTime;

            // HttpConstants의 로그 메시지 사용
            log.debug(LOG_RESPONSE, response.getStatusCode(), duration);

            return response;
        }
    }

    /**
     * 재시도 인터셉터
     * 실패 시 자동 재시도
     */
    @Slf4j
    public static class RetryInterceptor implements ClientHttpRequestInterceptor {

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {

            IOException lastException = null;

            // HttpConstants의 재시도 설정 사용
            for (int i = 0; i < MAX_RETRY_ATTEMPTS; i++) {
                try {
                    return execution.execute(request, body);

                } catch (IOException e) {
                    lastException = e;
                    // HttpConstants의 로그 메시지 사용
                    log.warn(LOG_RETRY, i + 1, MAX_RETRY_ATTEMPTS, e.getMessage());

                    if (i < MAX_RETRY_ATTEMPTS - 1) {
                        try {
                            // HttpConstants의 재시도 지연 시간과 백오프 사용
                            long delay = (long) (RETRY_DELAY_MS * Math.pow(RETRY_BACKOFF_MULTIPLIER, i));
                            Thread.sleep(delay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new IOException("재시도 중 인터럽트", ie);
                        }
                    }
                }
            }

            throw lastException;
        }
    }

    /**
     * 헤더 인터셉터
     * 공통 헤더 추가
     */
    @Slf4j
    public static class HeaderInterceptor implements ClientHttpRequestInterceptor {

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {

            // HttpConstants의 헤더 이름 사용
            request.getHeaders().add(HEADER_USER_AGENT, DEFAULT_USER_AGENT);

            // Content-Type이 없으면 JSON으로 설정
            if (!request.getHeaders().containsKey(HEADER_CONTENT_TYPE)) {
                request.getHeaders().add(HEADER_CONTENT_TYPE, MEDIA_TYPE_JSON);
            }

            // Accept 헤더 설정
            if (!request.getHeaders().containsKey(HEADER_ACCEPT)) {
                request.getHeaders().add(HEADER_ACCEPT, MEDIA_TYPE_JSON);
            }

            // Request ID 추가 (추적용)
            request.getHeaders().add(HEADER_X_REQUEST_ID, generateRequestId());

            return execution.execute(request, body);
        }

        private String generateRequestId() {
            return java.util.UUID.randomUUID().toString();
        }
    }

    /**
     * 커스텀 에러 핸들러
     * HTTP 에러 처리
     */
    @Slf4j
    public static class CustomResponseErrorHandler extends DefaultResponseErrorHandler {

        /**
         * 에러 처리
         * Spring Boot 3.x 방식
         */
        @Override
        public void handleError(ClientHttpResponse response) throws IOException {
            HttpStatusCode statusCode = response.getStatusCode();
            String statusText = response.getStatusText();
            byte[] body = getResponseBody(response);
            String bodyText = new String(body, StandardCharsets.UTF_8);

            // HttpConstants의 로그 메시지 사용
            log.error(LOG_ERROR, statusCode, bodyText);

            // 상태 코드에 따른 예외 처리
            if (statusCode.is4xxClientError()) {
                throw new HttpClientErrorException(
                        HttpStatus.valueOf(statusCode.value()),
                        statusText,
                        response.getHeaders(),
                        body,
                        StandardCharsets.UTF_8
                );
            } else if (statusCode.is5xxServerError()) {
                throw new HttpServerErrorException(
                        HttpStatus.valueOf(statusCode.value()),
                        statusText,
                        response.getHeaders(),
                        body,
                        StandardCharsets.UTF_8
                );
            } else {
                throw new RestClientException(
                        String.format("Unknown status code [%s]", statusCode)
                );
            }
        }

        /**
         * 응답 본문 읽기
         * 버퍼링된 스트림에서 안전하게 읽기
         */
        @Override
        protected byte[] getResponseBody(ClientHttpResponse response) {
            try {
                return StreamUtils.copyToByteArray(response.getBody());
            } catch (IOException ex) {
                // 응답 본문을 읽을 수 없는 경우 로깅만
                log.debug("응답 본문을 읽을 수 없음", ex);
                return new byte[0];
            }
        }
    }

    /**
     * 특정 서비스용 RestTemplate 생성 메소드
     * 서비스별로 다른 설정이 필요한 경우 사용
     */
    public RestTemplate createCustomRestTemplate(int connectTimeout, int readTimeout) {
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        factory.setConnectionRequestTimeout(Duration.ofMillis(CONNECTION_REQUEST_TIMEOUT_MS));

        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.getMessageConverters().add(0,
                new StringHttpMessageConverter(StandardCharsets.UTF_8));

        return restTemplate;
    }

}

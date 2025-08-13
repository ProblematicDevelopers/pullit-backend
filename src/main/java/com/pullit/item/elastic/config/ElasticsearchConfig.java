package com.pullit.item.elastic.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class ElasticsearchConfig {

    @Value("${ELASTICSEARCH_URIS:${ES_HOSTS:${elasticsearch.uris:}}}")
    private String[] uris;

    @Value("${elasticsearch.username:}")
    private String username;

    @Value("${elasticsearch.password:}")
    private String password;

    @Value("${elasticsearch.connectTimeout:5000}")
    private int connectTimeout;

    @Value("${elasticsearch.socketTimeout:60000}")
    private int socketTimeout;

    @Value("${elasticsearch.maxConnTotal:100}")
    private int maxConnTotal;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // 1. URI → HttpHost 배열 변환
        HttpHost[] hosts = Arrays.stream(uris)
                .map(HttpHost::create)
                .toArray(HttpHost[]::new);

        // 2. 인증 설정 (username/password가 모두 설정된 경우만 적용)
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        if (!username.isEmpty() && !password.isEmpty()) {
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));
        }

        // 3. RestClient 생성
        RestClient restClient = RestClient.builder(hosts)
                .setRequestConfigCallback(requestConfigBuilder ->
                        requestConfigBuilder
                                .setConnectTimeout(connectTimeout)
                                .setSocketTimeout(socketTimeout)
                )
                .setHttpClientConfigCallback(httpClientBuilder -> {
                    if (!username.isEmpty() && !password.isEmpty()) {
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                    return httpClientBuilder.setMaxConnTotal(maxConnTotal);
                })
                .build();

        // 4. Jackson ObjectMapper 커스터마이징
        ObjectMapper om = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        // 5. ElasticsearchClient 생성
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper(om));
        return new ElasticsearchClient(transport);
    }
}
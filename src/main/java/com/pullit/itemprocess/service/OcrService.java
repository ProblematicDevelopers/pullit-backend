package com.pullit.itemprocess.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullit.itemprocess.config.OcrProperties;
import com.pullit.itemprocess.dto.request.ClovaRequest;
import com.pullit.itemprocess.dto.request.MathpixRequest;
import com.pullit.itemprocess.dto.response.ClovaResponse;
import com.pullit.itemprocess.dto.response.MathpixResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;


@Service
@RequiredArgsConstructor
public class OcrService {
    private final OcrProperties ocrProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();


    public ClovaResponse callClovaOcrApi(ClovaRequest request) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.set(ocrProperties.getClova().getHeaderName(),
                ocrProperties.getClova().getSecretKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        // DTO → JSON 변환
        String body = objectMapper.writeValueAsString(request);

        // JSON 문자열을 entity에 넣어야 함
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        return restTemplate.exchange(
                ocrProperties.getClova().getInvokeUrl(),
                HttpMethod.POST,
                entity,
                ClovaResponse.class
        ).getBody();
    }

    public MathpixResponse callMathpixOcrApi(String imageUrlOrBase64) throws JsonProcessingException {
        // 요청 DTO 구성
        MathpixRequest request = new MathpixRequest();
        request.setSrc(imageUrlOrBase64);  // URL 또는 data:image/... base64 가능
        request.setFormats(List.of("text", "data"));

        MathpixRequest.DataOptions dataOptions = new MathpixRequest.DataOptions();
        dataOptions.setIncludeAsciimath(true);
        request.setDataOptions(dataOptions);

        // 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set(ocrProperties.getMathpix().getAppIdHeaderName(), ocrProperties.getMathpix().getAppId());
        headers.set(ocrProperties.getMathpix().getHeaderName(), ocrProperties.getMathpix().getAppKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 바디 직렬화
        String body = objectMapper.writeValueAsString(request);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        // API 호출
        return restTemplate.exchange(
                ocrProperties.getMathpix().getUrl(),
                HttpMethod.POST,
                entity,
                MathpixResponse.class
        ).getBody();
    }

}


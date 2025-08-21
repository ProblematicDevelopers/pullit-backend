package com.pullit.itemprocess.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ocr")
public class OcrProperties {
    private Clova clova = new Clova();
    private Mathpix mathpix = new Mathpix();

    @Data
    public static class Clova {
        private String invokeUrl;
        private String secretKey;
        private String headerName;
    }
    @Data
    public static class Mathpix {
        private String url;
        private String appId;
        private String appKey;
        private String headerName;
        private String appIdHeaderName;
    }
}

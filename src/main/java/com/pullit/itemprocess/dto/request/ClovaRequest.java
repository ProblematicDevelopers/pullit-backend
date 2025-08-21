package com.pullit.itemprocess.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class ClovaRequest {
    private List<Image> images;
    private String lang;
    private String requestId;
    private String resultType;
    private long timestamp;
    private String version;

    @Data
    public static class Image {
        private String format;
        private String name;
        private String data; // base64 or null
        private String url;
    }
}
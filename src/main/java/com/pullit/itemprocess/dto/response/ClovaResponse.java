package com.pullit.itemprocess.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ClovaResponse {
    private String version;
    private String requestId;
    private long timestamp;
    private List<ImageResult> images;

    @Data
    public static class ImageResult {
        private String uid;
        private String name;
        private String inferResult;
        private String message;
        private ValidationResult validationResult;
        private List<Field> fields;
    }

    @Data
    public static class ValidationResult {
        private String result;
    }

    @Data
    public static class Field {
        private String valueType;
        private BoundingPoly boundingPoly;
        private String inferText;
        private double inferConfidence;
    }

    @Data
    public static class BoundingPoly {
        private List<Vertex> vertices;
    }

    @Data
    public static class Vertex {
        private double x;
        private double y;
    }
}
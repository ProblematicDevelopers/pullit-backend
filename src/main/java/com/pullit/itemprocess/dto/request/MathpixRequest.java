package com.pullit.itemprocess.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class MathpixRequest {
    private String src;
    private List<String> formats;
    @JsonProperty("data_options")
    private DataOptions dataOptions;

    @Data
    public static class DataOptions {
        @JsonProperty("include_asciimath")
        private boolean includeAsciimath;
    }
}

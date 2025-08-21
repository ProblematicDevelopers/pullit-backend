package com.pullit.itemprocess.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MathpixResponse {
    @JsonProperty("request_id")
    private String requestId;

    private String version;

    @JsonProperty("is_printed")
    private boolean printed;

    @JsonProperty("is_handwritten")
    private boolean handwritten;

    private String text;

    @JsonProperty("latex_styled")
    private String latexStyled;
    private List<Map<String, String>> data;
}


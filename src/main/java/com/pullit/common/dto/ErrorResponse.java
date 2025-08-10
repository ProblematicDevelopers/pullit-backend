package com.pullit.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pullit.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String code;
    private String message;
    private String detail;
    private String path;
    private LocalDateTime timestamp;
    private Map<String, String> fieldErrors;
    private List<String> globalErrors;
    private int status;
    private String traceId;

    public static ErrorResponse of(String code, String message, String path, int status) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .path(path)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ErrorResponse of(String code, String message, String detail, String path, int status) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .detail(detail)
                .path(path)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ErrorResponse ofFieldErrors(String code, String message, Map<String, String> fieldErrors, String path, int status) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .fieldErrors(fieldErrors)
                .path(path)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ErrorResponse from(ErrorCode errorCode, String path) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .path(path)
                .status(errorCode.getHttpStatus().value())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ErrorResponse from(ErrorCode errorCode, String detail, String path) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .detail(detail)
                .path(path)
                .status(errorCode.getHttpStatus().value())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public ErrorResponse withTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

}

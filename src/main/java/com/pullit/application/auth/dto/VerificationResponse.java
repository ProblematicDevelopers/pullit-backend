package com.pullit.application.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationResponse {
    private boolean success;
    private String message;
    private Boolean verified;
}
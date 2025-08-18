package com.pullit.common.exception;

public class RateLimitExceededException extends BusinessException{
    public RateLimitExceededException() {
        super(ErrorCode.TOO_MANY_REQUESTS);
    }

    public RateLimitExceededException(String message) {
        super(ErrorCode.TOO_MANY_REQUESTS, message);
    }
}

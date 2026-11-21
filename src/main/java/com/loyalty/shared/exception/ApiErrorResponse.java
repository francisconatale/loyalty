package com.loyalty.shared.exception;

import java.time.Instant;

public record ApiErrorResponse(
    String errorCode,
    String message,
    String path,
    Instant timestamp
) {
    public static ApiErrorResponse of(String errorCode, String message, String path) {
        return new ApiErrorResponse(errorCode, message, path, Instant.now());
    }
}

package com.condos.shared.dto;

import java.time.Instant;

/**
 * Para estandarizar los errores devueltos por la API.
 */
public record ErrorResponse(
        String message,
        int status,
        Instant timestamp
) {
    public static ErrorResponse of(String message, int status) {
        return new ErrorResponse(message, status, Instant.now());
    }
}
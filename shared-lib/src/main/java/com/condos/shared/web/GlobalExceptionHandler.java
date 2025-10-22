package com.condos.shared.web;

import com.condos.shared.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

@ControllerAdvice
public class GlobalExceptionHandler {

    private final boolean verbose;

    public GlobalExceptionHandler(Environment env) {
        // Activa mensajes detallados sólo en perfiles dev/local si quieres
        this.verbose = Optional.ofNullable(env.getProperty("app.errors.verbose"))
                .map(Boolean::parseBoolean).orElse(false);
    }

    // 401/403/404/400 que lances con ResponseStatusException
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleRSE(ResponseStatusException ex) {
        HttpStatus status = ex.getStatusCode() instanceof HttpStatus hs ? hs : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(
                new ErrorResponse(messageOf(ex), status.value(), Instant.now())
        );
    }

    // 403 de Spring Security
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleDenied(AccessDeniedException ex) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status).body(
                new ErrorResponse(messageOf(ex), status.value(), Instant.now())
        );
    }

    // Parámetro requerido faltante: ?page=, ?q=, etc.
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String msg = "Required request parameter '" + ex.getParameterName() + "' is missing";
        return ResponseEntity.status(status).body(
                new ErrorResponse(verbose ? msg : "Bad request", status.value(), Instant.now())
        );
    }

    // Mismatch de tipos (?limit=abc y el controlador espera int)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String msg = "Invalid value for parameter '" + ex.getName() + "'";
        return ResponseEntity.status(status).body(
                new ErrorResponse(verbose ? msg : "Bad request", status.value(), Instant.now())
        );
    }

    // Violaciones de validación de jakarta.validation (@Min, @NotBlank, etc.)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String msg = ex.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .orElse("Validation error");
        return ResponseEntity.status(status).body(
                new ErrorResponse(verbose ? msg : "Validation error", status.value(), Instant.now())
        );
    }

    // Fallback 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(
                new ErrorResponse(verbose ? messageOf(ex) : "Internal error", status.value(), Instant.now())
        );
    }

    private String messageOf(Throwable t) {
        String m = t.getMessage();
        return (m == null || m.isBlank()) ? t.getClass().getSimpleName() : m;
    }
}
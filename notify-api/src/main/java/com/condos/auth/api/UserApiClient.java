package com.condos.auth.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Component
public class UserApiClient {

    private final WebClient http;
    private final String internalSecret;

    // SUGERENCIA: configura user.api.url = http://localhost:8081/condos/api/user
    public UserApiClient(@Value("${user.api.url}") String baseUrl,
                         @Value("${user.api.secret:}") String internalSecret,
                         WebClient.Builder builder) {
        this.http = builder.baseUrl(baseUrl).build();
        this.internalSecret = internalSecret;
    }

    /** NUEVO: trae assignments por email (Opción A) */
    public Mono<List<OrgAssignment>> getAssignmentsByEmail(String email) {
        //final String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        //final String encoded = URLEncoder.encode(normalized, StandardCharsets.UTF_8);

        return http.get()
                .uri(uri -> uri
                        // Si baseUrl = http://.../condos/api/user
                        // el path queda: /internal/users/by-email/{email}/assignments
                        .path("/internal/users/by-email/{email}/assignments")
                        .build(email))
                .accept(MediaType.APPLICATION_JSON)
                .headers(h -> {
                    if (!internalSecret.isBlank()) {
                        h.add("X-Internal-Secret", internalSecret);
                    }
                })
                .retrieve()
                // Propaga 4xx con cuerpo para logging
                .onStatus(HttpStatusCode::is4xxClientError, rsp ->
                        rsp.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException(
                                        "user-api 4xx: " + rsp.statusCode() + " - " + body))))
                .onStatus(HttpStatusCode::is5xxServerError, rsp ->
                        rsp.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException(
                                        "user-api 5xx: " + rsp.statusCode() + " - " + body))))
                .bodyToFlux(OrgAssignment.class)
                // Reintenta sólo fallas de red o 5xx (no 4xx)
                .retryWhen(Retry.backoff(2, Duration.ofMillis(200))
                        .filter(ex -> (ex instanceof WebClientRequestException) ||
                                (ex instanceof WebClientResponseException wre
                                        && wre.getStatusCode().is5xxServerError())))
                .collectList();
    }

    /** Si aún quieres el método por id, déjalo pero reactivo */
    public Mono<List<OrgAssignment>> getAssignmentsByUserId(String userId) {
        return http.get()
                .uri(uri -> uri.path("/internal/users/{id}/assignments").build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .headers(h -> { if (!internalSecret.isBlank()) h.add("X-Internal-Secret", internalSecret); })
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, rsp ->
                        rsp.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException(
                                        "user-api 4xx: " + rsp.statusCode() + " - " + body))))
                .onStatus(HttpStatusCode::is5xxServerError, rsp ->
                        rsp.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException(
                                        "user-api 5xx: " + rsp.statusCode() + " - " + body))))
                .bodyToFlux(OrgAssignment.class)
                .retryWhen(Retry.backoff(2, Duration.ofMillis(200))
                        .filter(ex -> (ex instanceof WebClientRequestException) ||
                                (ex instanceof WebClientResponseException wre
                                        && wre.getStatusCode().is5xxServerError())))
                .collectList();
    }

    /** DTO alineado con el user-api (orgId como String) */
    public record OrgAssignment(String orgId, String role, String status) {
        public boolean isActive() { return "ACTIVE".equalsIgnoreCase(status); }
    }
}
package com.condos.user.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class AuthApiClientImpl implements AuthApiClient {

    private final WebClient webClient;
    private final String internalSecret;

    public AuthApiClientImpl(WebClient.Builder builder,
                             @Value("${auth.api.base-url}") String baseUrl,
                             @Value("${auth.api.secret}") String internalSecret) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.internalSecret = internalSecret;
    }

    @Override
    public String provisionAccount(String email, String rawPassword) {
        record CreateReq(String email, String password) {}
        record Resp(String id, String email) {}

        Resp resp = webClient.post()
                .uri("/internal/accounts")
                .header("X-Internal-Secret", internalSecret)   // ✅ importante
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateReq(email, rawPassword))
                .retrieve()
                .bodyToMono(Resp.class)
                .block();

        if (resp == null || resp.id() == null) {
            throw new IllegalStateException("auth_account provisioning failed");
        }
        return resp.id();
    }

    @Override
    public Optional<String> findAccountIdByEmail(String email) {
        record Resp(String id, String email) {}
        return webClient.get()
                .uri(uri -> uri.path("/internal/accounts/by-email")
                        .queryParam("email", email).build())
                .header("X-Internal-Secret", internalSecret)   // ✅ importante
                .retrieve()
                .bodyToMono(Resp.class)
                .map(Resp::id)
                .onErrorResume(ex -> Mono.empty())
                .blockOptional();
    }
}
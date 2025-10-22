package com.condos.user.service;

import java.util.Optional;

public interface AuthApiClient {
    // devuelve el accountId o lanza excepción si falla
    String provisionAccount(String email, String rawPassword);
    Optional<String> findAccountIdByEmail(String email);
}
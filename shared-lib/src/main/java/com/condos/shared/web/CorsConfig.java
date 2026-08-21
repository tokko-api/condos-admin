package com.condos.shared.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS compartido para todos los microservicios (auth-api, tenant-api, user-api, board-api, ...).
 * Antes solo auth-api tenía esto definido localmente; se centraliza aquí para que
 * cualquier servicio nuevo (p. ej. billing-api) lo herede sin duplicar código,
 * y para que el resto de servicios (tenant-api, user-api, board-api) también
 * acepten llamadas desde el frontend web (Expo en localhost:8081, condosadmin.site).
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:8081",
                "http://condosadmin.site",
                "https://condosadmin.site",
                "https://www.condosadmin.site"
        ));

        config.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        config.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "ngrok-skip-browser-warning"
        ));

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

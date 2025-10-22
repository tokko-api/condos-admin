package com.condos.board.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())

                // 🔐 API stateless y sin request cache (adiós “Saved request … to session”)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(rc -> rc.disable())
                .securityContext(sc -> sc.requireExplicitSave(false))
                .exceptionHandling(eh -> eh.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health", "/actuator/health", "/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()

                        .requestMatchers(HttpMethod.GET, "/condos/api/board/reports/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/condos/api/board/reports").authenticated()
                        .requestMatchers(HttpMethod.GET, "/condos/api/board/tasks/stats/**").authenticated()

                        // ⚠️ con boardId en la ruta
                        .requestMatchers(HttpMethod.POST,
                                "/condos/api/board/*/tasks/*/attachments/presign",
                                "/condos/api/board/*/tasks/*/attachments/complete"
                        ).authenticated()

                        .requestMatchers(HttpMethod.POST, "/condos/api/board/boards/**").authenticated()
                        .requestMatchers("/condos/api/board/**").authenticated()
                        .anyRequest().authenticated()
                )

                // ⬅️ MUY IMPORTANTE: tu filtro antes del AnonymousAuthenticationFilter
                .addFilterBefore(jwtAuthFilter, AnonymousAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}
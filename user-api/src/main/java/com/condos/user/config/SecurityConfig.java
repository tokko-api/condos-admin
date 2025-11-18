package com.condos.user.config;

import com.condos.shared.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class SecurityConfig {

    private final JwtService jwtService;

    public SecurityConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwt) throws Exception {
        http.csrf(csrf -> csrf.disable());

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/internal/**").permitAll()
                .requestMatchers("/condos/api/user/internal/**").permitAll()// libre para auth-api
                .anyRequest().authenticated()                  // el resto con JWT
        );

        http.addFilterBefore(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain fc)
                    throws IOException, ServletException {
                String h = req.getHeader("Authorization");
                if (h != null && h.startsWith("Bearer ")) {
                    try {
                        var jws   = jwt.parse(h.substring(7));
                        var body  = jws.getBody();
                        var userId = body.getSubject();

                        // opcional: derivamos roles raíz por conveniencia
                        @SuppressWarnings("unchecked")
                        var orgsClaim = (List<Map<String, Object>>) body.get("orgs");
                        var rolesRoot = orgsClaim == null ? List.of() :
                                orgsClaim.stream().map(m -> String.valueOf(m.get("role"))).distinct().toList();

                        var auths = new ArrayList<GrantedAuthority>();
                        auths.add(new SimpleGrantedAuthority("ROLE_USER"));
                        if (rolesRoot.contains("SUPERADMIN")) {
                            auths.add(new SimpleGrantedAuthority("ROLE_SUPERADMIN"));
                        }

                        var auth = new UsernamePasswordAuthenticationToken(userId, null, auths);

                        // 👇 IMPORTANTE: copiar orgs a details para JwtAuth.orgs(...)
                        Map<String,Object> details = new HashMap<>();
                        details.put("email", body.get("email"));
                        details.put("orgs",  orgsClaim);        // <-- aquí
                        details.put("ver",   body.get("ver"));
                        details.put("roles", rolesRoot);        // opcional
                        auth.setDetails(details);

                        SecurityContextHolder.getContext().setAuthentication(auth);
                    } catch (Exception e) {
                        SecurityContextHolder.clearContext();
                    }
                }
                fc.doFilter(req, res);
            }
        }, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Filtro JWT simple: valida el token en Authorization: Bearer <token>
     */
    static class JwtAuthFilter extends OncePerRequestFilter {

        private final JwtService jwtService;

        JwtAuthFilter(JwtService jwtService) {
            this.jwtService = jwtService;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain)
                throws ServletException, IOException {

            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                try {
                    Jws<Claims> jws = jwtService.parse(token);
                    Claims claims = jws.getBody();

                    String userId = claims.getSubject();

                    // 1️⃣ Crea Authorities desde los roles del JWT
                    List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

                    Object rolesObj = claims.get("roles");
                    if (rolesObj instanceof java.util.Collection<?> col) {
                        for (Object r : col) {
                            String role = String.valueOf(r).toUpperCase();
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                        }
                    } else if (rolesObj != null) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + rolesObj.toString().toUpperCase()));
                    }

                    // 2️⃣ Construye la autenticación
                    var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);

                    // 3️⃣ Inserta los claims que usará JwtAuth
                    Map<String, Object> details = new java.util.HashMap<>();
                    details.put("orgs", claims.get("orgs"));
                    details.put("roles", claims.get("roles"));
                    details.put("email", claims.get("email"));
                    auth.setDetails(details);

                    // 4️⃣ Coloca la autenticación en el contexto
                    SecurityContextHolder.getContext().setAuthentication(auth);

                } catch (Exception ex) {
                    SecurityContextHolder.clearContext();
                }
            }

            filterChain.doFilter(request, response);
        }
    }
}
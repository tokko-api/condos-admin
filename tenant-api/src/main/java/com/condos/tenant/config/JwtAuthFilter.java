package com.condos.tenant.config;

import com.condos.shared.security.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;

    public JwtAuthFilter(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        var header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                var jws   = jwt.parse(header.substring(7));
                var user  = jws.getBody().getSubject();
                var orgId = jwt.extractOrgId(jws);
                var roles = jwt.extractRoles(jws);

                List<SimpleGrantedAuthority> auths = roles.stream()
                        .map(r -> r.equals("SUPERADMIN") ? r : r + "@" + orgId)
                        .map(SimpleGrantedAuthority::new)
                        .toList();

                var auth = new UsernamePasswordAuthenticationToken(user, null, auths);
                auth.setDetails(orgId);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException ignored) {
                // token inválido: lo ignoramos y dejamos que el chain siga (caerá en 401 si endpoint requiere auth)
            }
        }

        chain.doFilter(req, res);
    }
}
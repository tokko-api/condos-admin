package com.condos.shared.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * JWT util compartido:
 * - Nuevo formato: claims { email, orgs:[{orgId, role}], ver }
 * - Retrocompatibilidad: método generate(userId, email, orgId, roles) (DEPRECATED)
 */
@Service
public class JwtService {

    @Value("${app.security.jwt.secret}")  private String secret;
    @Value("${app.security.jwt.issuer}")  private String issuer;
    @Value("${app.security.jwt.expires-minutes}") private long expMin;

    // ========= NUEVO formato (multi-org + version) =========

    /**
     * Genera un JWT con múltiples organizaciones activas y número de versión.
     * Claims:
     *   email: String
     *   orgs : List<{orgId:String, role:String}>
     *   ver  : long (accountVersion para invalidar tokens viejos)
     */
    public String generate(String userId, String email, List<OrgRole> activeOrgs, long version) {
        var now = new Date();
        var expiry = Date.from(Instant.now().plus(Duration.ofMinutes(expMin)));

        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("orgs", activeOrgs == null ? List.of() :
                activeOrgs.stream().map(o -> Map.of("orgId", o.orgId(), "role", o.role())).toList());
        claims.put("ver", version);

        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(userId)
                .addClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))) // HS256 por defecto
                .compact();
    }

    // ========= LEGACY (mantener mientras migras servicios) =========

    /**
     * @deprecated Usa {@link #generate(String, String, List, long)} con orgs y ver.
     */
    @Deprecated
    public String generate(String userId, String email, String orgId, Collection<String> roles) {
        var now = new Date();
        var expiry = Date.from(Instant.now().plus(Duration.ofMinutes(expMin)));

        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("roles", roles == null ? List.of() : roles);
        claims.put("orgId", orgId);

        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(userId)
                .addClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    // ========= Parse / helpers =========

    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder()
                .requireIssuer(issuer)
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token);
    }

    // --- Helpers LEGACY ---
    public List<String> extractRoles(Jws<Claims> jws) {
        Object roles = jws.getBody().get("roles");
        return (roles instanceof List<?> l) ? l.stream().map(String::valueOf).toList() : List.of();
    }

    public String extractOrgId(Jws<Claims> jws) {
        return jws.getBody().get("orgId", String.class);
    }

    public JwtPayload toPayload(Jws<Claims> jws) {
        // Mantiene compatibilidad con el payload viejo (orgId + roles)
        String sub   = jws.getBody().getSubject();
        String orgId = extractOrgId(jws);
        List<String> roles = extractRoles(jws);
        return new JwtPayload(sub, orgId, roles);
    }

    // --- Helpers NUEVO formato ---
    public List<OrgRole> extractOrgRoles(Jws<Claims> jws) {
        Object orgs = jws.getBody().get("orgs");
        if (!(orgs instanceof List<?> list)) return List.of();
        List<OrgRole> result = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof Map<?,?> m) {
                String orgId = String.valueOf(m.get("orgId"));
                String role  = String.valueOf(m.get("role"));
                if (orgId != null && role != null) {
                    result.add(new OrgRole(orgId, role));
                }
            }
        }
        return result;
    }

    public long extractVersion(Jws<Claims> jws) {
        Object v = jws.getBody().get("ver");
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception ignored) { return 1L; }
    }

    // ========= DTOs compartidos =========

    /** Par {orgId, role} para el claim "orgs". */
    public record OrgRole(String orgId, String role) {}

    /** Payload legacy (orgId + roles) usado por servicios antiguos. */
    public record JwtPayload(String userId, String orgId, List<String> roles) {}
}
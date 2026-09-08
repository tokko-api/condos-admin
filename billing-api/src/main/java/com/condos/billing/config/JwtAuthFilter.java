package com.condos.billing.config;

import com.condos.shared.security.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * IMPORTANTE: auth-api emite JWTs en formato NUEVO (multi-org):
 * claims { email, orgs: [{orgId, role}], ver }.
 * Este filtro debe leer ese claim "orgs" y ponerlo en auth.getDetails() como
 * un Map con clave "orgs" (lista de {orgId, role}), que es justo lo que
 * JwtAuth.claims(...) espera para resolver hasRoleInOrg/hasAccessToBoard/
 * hasAccessToTask/hasAccessToUnit/isSuperadmin.
 *
 * Antes este filtro leía el formato LEGADO (orgId/roles sueltos, ya no
 * emitido por auth-api), por lo que esos métodos de JwtAuth siempre
 * devolvían false y cualquier @PreAuthorize que dependiera de ellos fallaba
 * en silencio (403), incluyendo boards, tasks y units.
 */
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
                var jws = jwt.parse(header.substring(7));
                var userId = jws.getBody().getSubject();

                List<JwtService.OrgRole> orgRoles = jwt.extractOrgRoles(jws);

                List<Map<String, Object>> orgsForDetails = new ArrayList<>();
                List<String> rolesRoot = new ArrayList<>();
                List<GrantedAuthority> auths = new ArrayList<>();
                auths.add(new SimpleGrantedAuthority("ROLE_USER"));

                for (var o : orgRoles) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("orgId", o.orgId());
                    m.put("role", o.role());
                    orgsForDetails.add(m);
                    rolesRoot.add(o.role());

                    auths.add(new SimpleGrantedAuthority(
                            "SUPERADMIN".equals(o.role()) ? o.role() : o.role() + "@" + o.orgId()));
                    if ("SUPERADMIN".equals(o.role())) {
                        auths.add(new SimpleGrantedAuthority("ROLE_SUPERADMIN"));
                    }
                }

                var auth = new UsernamePasswordAuthenticationToken(userId, null, auths);
                Map<String, Object> details = new HashMap<>();
                details.put("orgs", orgsForDetails);
                details.put("roles", rolesRoot);
                // Se conserva el token crudo para poder reenviarlo a board-api al resolver
                // el ownership de una unidad (ver JwtAuth.isOwnUnit / BoardApiClient).
                details.put("token", header.substring(7));
                auth.setDetails(details);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException ignored) {
                // token inválido: lo ignoramos y dejamos que el chain siga (caerá en 401 si el endpoint requiere auth)
            }
        }

        chain.doFilter(req, res);
    }
}

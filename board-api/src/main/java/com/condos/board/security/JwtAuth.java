package com.condos.board.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.*;

@Component("jwtAuth")
public class JwtAuth {

    @SuppressWarnings("unchecked")
    private Map<String, Object> claims(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jat) {
            return jat.getTokenAttributes(); // ← estándar con resource server
        }
        Object details = auth != null ? auth.getDetails() : null;
        if (details instanceof Map<?,?> map) {
            // convertir a Map<String,Object> seguro
            Map<String,Object> out = new HashMap<>();
            map.forEach((k,v) -> out.put(String.valueOf(k), v));
            return out;
        }
        return Collections.emptyMap();
    }

    /** ¿Tiene rol SUPERADMIN global? */
    public boolean isSuperadmin(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return false;

        var c = claims(auth);

        Object roles = c.get("roles");
        if (roles instanceof Collection<?> col) {
            for (Object r : col) {
                if ("SUPERADMIN".equals(String.valueOf(r))) return true;
            }
        }
        // fallback: algunos tokens solo llevan roles por org
        Object orgs = c.get("orgs");
        if (orgs instanceof Collection<?> col) {
            for (Object o : col) {
                if (o instanceof Map<?,?> m) {
                    String role = String.valueOf(m.get("role"));
                    if ("SUPERADMIN".equals(role)) return true;
                }
            }
        }
        return false;
    }

    /** ¿Tiene alguno de los roles requeridos dentro de ese orgId? */
    public boolean hasRoleInOrg(Authentication auth, String orgId, Collection<String> rolesReq) {
        if (auth == null || !auth.isAuthenticated()) return false;

        var c = claims(auth);
        Object orgs = c.get("orgs");
        if (!(orgs instanceof Collection<?> col)) return false;

        for (Object o : col) {
            if (o instanceof Map<?,?> m) {
                String oid  = String.valueOf(m.get("orgId"));
                String role = String.valueOf(m.get("role"));
                if (Objects.equals(orgId, oid) && rolesReq.contains(role)) {
                    return true;
                }
            }
        }
        return false;
    }
}
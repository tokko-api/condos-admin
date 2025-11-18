package com.condos.user.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.*;

@Component("jwtAuth")
public class JwtAuth {

    private static final Map<String, Integer> RANK = Map.of(
            "SUPERADMIN", 3,
            "ADMINISTRADOR", 2,
            "SUPERVISOR", 1,
            "OPERATIVO", 0
    );

    public boolean isSuperadmin(Authentication auth) {
        if (auth == null) return false;
        // por authorities
        if (auth.getAuthorities() != null &&
                auth.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_SUPERADMIN".equalsIgnoreCase(a.getAuthority()))) {
            return true;
        }
        // por orgs en details (tu lógica actual)
        for (Map<String, String> m : orgs(auth)) {
            if ("SUPERADMIN".equalsIgnoreCase(m.get("role"))) return true;
        }
        return false;
    }

    public boolean hasRoleInOrg(Authentication auth, String orgId, Collection<String> allowed) {
        if (orgId == null || allowed == null || allowed.isEmpty()) return false;
        for (Map<String, String> m : orgs(auth)) {
            if (orgId.equals(m.get("orgId")) && allowed.contains(m.get("role"))) return true;
        }
        return false;
    }

    /**
     * Verifica si el usuario autenticado puede gestionar (editar/borrar) a otro
     * usuario con cierto rol dentro de un orgId.
     */
    public boolean canManage(Authentication auth, String orgId, String targetRole) {
        String myRole = roleInOrg(auth, orgId);
        if ("SUPERADMIN".equals(myRole)) return true; // siempre puede
        return RANK.getOrDefault(myRole, 0) > RANK.getOrDefault(targetRole, 0);
    }

    /**
     * Devuelve el rol del usuario en un orgId específico.
     */
    public String roleInOrg(Authentication auth, String orgId) {
        if (auth == null || orgId == null) return "OPERATIVO";
        for (Map<String, String> m : orgs(auth)) {
            if (orgId.equals(m.get("orgId"))) return m.get("role");
        }
        return "OPERATIVO";
    }

    /**
     * Extrae el userId (sub) del principal.
     */
    public String userId(Authentication auth) {
        return auth == null ? null : String.valueOf(auth.getPrincipal());
    }

    /**
     * Obtiene todos los orgs del token como lista de maps {orgId,role}.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> orgs(Authentication auth) {
        if (auth == null) return List.of();
        Object details = auth.getDetails();
        if (!(details instanceof Map<?, ?> det)) return List.of();

        Object raw = det.get("orgs");
        if (!(raw instanceof List<?> list)) return List.of();

        List<Map<String, String>> out = new ArrayList<>(list.size());
        for (Object o : list) {
            if (o instanceof Map<?, ?> m) {
                String orgId = valStr(m.get("orgId"));
                String role = valStr(m.get("role"));
                if (orgId != null && role != null) {
                    out.add(Map.of("orgId", orgId, "role", role));
                }
            }
        }
        return out;
    }

    private static String valStr(Object o) {
        return (o == null) ? null : String.valueOf(o);
    }
}
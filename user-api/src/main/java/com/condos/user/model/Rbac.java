package com.condos.user.model;

import com.condos.user.security.JwtAuth;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component("rbac")
public class Rbac {

    private final JwtAuth jwtAuth; // ← inyectamos JwtAuth

    public Rbac(JwtAuth jwtAuth) {
        this.jwtAuth = jwtAuth;
    }
    private static final Map<String,Integer> RANK = Map.of(
            "SUPERADMIN", 3, "ADMINISTRADOR", 2, "SUPERVISOR", 1, "OPERATIVO", 0
    );

    public boolean canManage(String orgId, String targetRole, Authentication auth) {
        if (orgId == null || auth == null || targetRole == null) return false;

        String tRole = targetRole.toUpperCase(Locale.ROOT);
        String myRole = jwtAuth.roleInOrg(auth, orgId).toUpperCase(Locale.ROOT);

        if ("SUPERADMIN".equals(myRole)) return true;

        return RANK.getOrDefault(myRole, 0) > RANK.getOrDefault(tRole, 0);
    }
}
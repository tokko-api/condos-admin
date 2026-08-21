package com.condos.board.security;

import com.condos.board.repository.BoardRepository;
import com.condos.board.repository.TaskRepository;
import com.condos.board.repository.UnitRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.*;

@Component("jwtAuth")
public class JwtAuth {

    private final BoardRepository boards;
    private final TaskRepository tasks;
    private final UnitRepository units;

    public JwtAuth(BoardRepository boards, TaskRepository tasks, UnitRepository units) {
        this.boards = boards;
        this.tasks = tasks;
        this.units = units;
    }

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

    /** ¿Tiene alguno de los roles requeridos en el org dueño de este board? */
    public boolean hasAccessToBoard(Authentication auth, String boardId, Collection<String> rolesReq) {
        if (boardId == null) return false;
        var board = boards.findById(boardId).orElse(null);
        if (board == null) return false;
        return hasRoleInOrg(auth, board.orgId, rolesReq);
    }

    /** ¿Tiene alguno de los roles requeridos en el org dueño del board al que pertenece esta task? */
    public boolean hasAccessToTask(Authentication auth, String taskId, Collection<String> rolesReq) {
        if (taskId == null) return false;
        var task = tasks.findById(taskId).orElse(null);
        if (task == null) return false;
        return hasRoleInOrg(auth, task.getOrgId(), rolesReq);
    }

    /** ¿Tiene alguno de los roles requeridos en el org dueño de la unidad? */
    public boolean hasAccessToUnit(Authentication auth, String unitId, Collection<String> rolesReq) {
        if (unitId == null) return false;
        var unit = units.findById(unitId).orElse(null);
        if (unit == null) return false;
        return hasRoleInOrg(auth, unit.getOrgId(), rolesReq);
    }
}
package com.condos.billing.security;

import com.condos.billing.repository.ChargeRepository;
import com.condos.billing.repository.ExpenseRepository;
import com.condos.billing.repository.FeeScheduleRepository;
import com.condos.billing.repository.PaymentRepository;
import com.condos.billing.service.BoardApiClient;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.*;

@Component("jwtAuth")
public class JwtAuth {

    private final FeeScheduleRepository schedules;
    private final ChargeRepository charges;
    private final PaymentRepository payments;
    private final ExpenseRepository expenses;
    private final BoardApiClient boardApi;

    public JwtAuth(FeeScheduleRepository schedules, ChargeRepository charges, PaymentRepository payments,
                    ExpenseRepository expenses, BoardApiClient boardApi) {
        this.schedules = schedules;
        this.charges = charges;
        this.payments = payments;
        this.expenses = expenses;
        this.boardApi = boardApi;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> claims(Authentication auth) {
        // billing-api siempre coloca un Map en auth.getDetails() (ver JwtAuthFilter);
        // no usamos Spring's JwtAuthenticationToken/oauth2-resource-server aquí.
        Object details = auth != null ? auth.getDetails() : null;
        if (details instanceof Map<?, ?> map) {
            Map<String, Object> out = new HashMap<>();
            map.forEach((k, v) -> out.put(String.valueOf(k), v));
            return out;
        }
        return Collections.emptyMap();
    }

    public boolean isSuperadmin(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return false;
        var c = claims(auth);

        Object roles = c.get("roles");
        if (roles instanceof Collection<?> col) {
            for (Object r : col) {
                if ("SUPERADMIN".equals(String.valueOf(r))) return true;
            }
        }
        Object orgs = c.get("orgs");
        if (orgs instanceof Collection<?> col) {
            for (Object o : col) {
                if (o instanceof Map<?, ?> m && "SUPERADMIN".equals(String.valueOf(m.get("role")))) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasRoleInOrg(Authentication auth, String orgId, Collection<String> rolesReq) {
        if (auth == null || !auth.isAuthenticated()) return false;
        var c = claims(auth);
        Object orgs = c.get("orgs");
        if (!(orgs instanceof Collection<?> col)) return false;

        for (Object o : col) {
            if (o instanceof Map<?, ?> m) {
                String oid = String.valueOf(m.get("orgId"));
                String role = String.valueOf(m.get("role"));
                if (Objects.equals(orgId, oid) && rolesReq.contains(role)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** ¿Tiene alguno de los roles requeridos en el org dueño de esta configuración de cuota? */
    public boolean hasAccessToSchedule(Authentication auth, String scheduleId, Collection<String> rolesReq) {
        if (scheduleId == null) return false;
        var s = schedules.findById(scheduleId).orElse(null);
        if (s == null) return false;
        return hasRoleInOrg(auth, s.getOrgId(), rolesReq);
    }

    /** ¿Tiene alguno de los roles requeridos en el org dueño de este cargo? */
    public boolean hasAccessToCharge(Authentication auth, String chargeId, Collection<String> rolesReq) {
        if (chargeId == null) return false;
        var c = charges.findById(chargeId).orElse(null);
        if (c == null) return false;
        return hasRoleInOrg(auth, c.getOrgId(), rolesReq);
    }

    /** ¿Tiene alguno de los roles requeridos en el org dueño de este pago? */
    public boolean hasAccessToPayment(Authentication auth, String paymentId, Collection<String> rolesReq) {
        if (paymentId == null) return false;
        var p = payments.findById(paymentId).orElse(null);
        if (p == null) return false;
        return hasRoleInOrg(auth, p.getOrgId(), rolesReq);
    }

    /** ¿Tiene alguno de los roles requeridos en el org dueño de este egreso? */
    public boolean hasAccessToExpense(Authentication auth, String expenseId, Collection<String> rolesReq) {
        if (expenseId == null) return false;
        var e = expenses.findById(expenseId).orElse(null);
        if (e == null) return false;
        return hasRoleInOrg(auth, e.getOrgId(), rolesReq);
    }

    /**
     * ¿Es el condomino/residente asignado a esta unidad? Resuelve la unidad en
     * board-api reenviando el token del propio usuario (misma limitación de
     * auth servicio-a-servicio documentada en BoardApiClient).
     */
    public boolean isOwnUnit(Authentication auth, String unitId) {
        if (unitId == null || auth == null || !auth.isAuthenticated()) return false;
        var c = claims(auth);
        Object token = c.get("token");
        if (!(token instanceof String bearerToken) || bearerToken.isBlank()) return false;

        var unit = boardApi.getUnit(unitId, bearerToken);
        return unit != null && auth.getName().equals(unit.residentUserId());
    }
}

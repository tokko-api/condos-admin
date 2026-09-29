package com.condos.billing.api;

import com.condos.billing.service.PaymentReminderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/condos/api/billing")
@RequiredArgsConstructor
public class PaymentReminderController {

    private final PaymentReminderService reminders;

    @PostMapping("/units/{unitId}/payment-reminder")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasRoleInOrg(authentication, #orgId, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})
    """)
    public Map<String, Object> send(@RequestParam String orgId, @PathVariable String unitId, HttpServletRequest http) {
        reminders.sendReminder(unitId, bearerToken(http));
        return Map.of("sent", true);
    }

    private String bearerToken(HttpServletRequest http) {
        String h = http.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) return h.substring(7);
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing bearer token");
    }
}

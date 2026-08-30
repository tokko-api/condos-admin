package com.condos.billing.api;

import com.condos.billing.api.dto.BoardCollectionRes;
import com.condos.billing.stats.BillingStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/condos/api/billing/stats")
@RequiredArgsConstructor
public class BillingStatsController {

    private final BillingStatsService stats;

    /**
     * Cobranza del mes agrupada por condominio (boardId).
     * `period` es opcional, formato "yyyy-MM" (ej. "2026-08"); si se omite,
     * se usa el mes actual (UTC). El nombre del condominio se resuelve en
     * el frontend contra la lista de boards que ya tiene cargada.
     */
    @GetMapping("/collection-by-board")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasRoleInOrg(authentication, #orgId, {'ADMINISTRADOR','SUPERVISOR'})
    """)
    public List<BoardCollectionRes> collectionByBoard(
            @RequestParam String orgId,
            @RequestParam(required = false) String period) {
        return stats.collectionByBoard(orgId, period);
    }
}

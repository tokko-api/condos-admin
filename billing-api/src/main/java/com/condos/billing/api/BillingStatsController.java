package com.condos.billing.api;

import com.condos.billing.api.dto.BoardCollectionRes;
import com.condos.billing.api.dto.BoardExpenseRes;
import com.condos.billing.api.dto.ExpenseCategoryBreakdownRes;
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

    /**
     * Cobranza del mes de UNA colonia (boardId), para que un condómino pueda
     * ver el dato de su propia colonia sin exponerle la cobranza de otras
     * colonias de la organización (a diferencia de collection-by-board, que
     * es una vista administrativa de toda la organización).
     */
    @GetMapping("/collection-by-board/mine")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasRoleInOrg(authentication, #orgId, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'}) or
        @jwtAuth.isResidentOfBoard(authentication, #boardId)
    """)
    public BoardCollectionRes collectionForMyBoard(
            @RequestParam String orgId,
            @RequestParam String boardId,
            @RequestParam(required = false) String period) {
        return stats.collectionByBoard(orgId, period).stream()
                .filter(r -> boardId.equals(r.boardId()))
                .findFirst()
                .orElse(new BoardCollectionRes(boardId, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, 0.0));
    }

    /**
     * Egresos del mes agrupados por condominio (boardId). Mismo formato de
     * `period` que collection-by-board.
     */
    @GetMapping("/expenses-by-board")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasRoleInOrg(authentication, #orgId, {'ADMINISTRADOR','SUPERVISOR'})
    """)
    public List<BoardExpenseRes> expensesByBoard(
            @RequestParam String orgId,
            @RequestParam(required = false) String period) {
        return stats.expensesByBoard(orgId, period);
    }

    /** Desglose de egresos por categoría para una colonia (boardId) en un periodo. */
    @GetMapping("/expenses-by-category")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasRoleInOrg(authentication, #orgId, {'ADMINISTRADOR','SUPERVISOR'})
    """)
    public List<ExpenseCategoryBreakdownRes> expensesByCategory(
            @RequestParam String orgId,
            @RequestParam(required = false) String boardId,
            @RequestParam(required = false) String period) {
        return stats.expensesByCategory(orgId, boardId, period);
    }
}

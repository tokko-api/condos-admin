package com.condos.billing.api;

import com.condos.billing.api.dto.AccountStatementResponse;
import com.condos.billing.service.AccountStatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/condos/api/billing")
@RequiredArgsConstructor
public class AccountStatementController {

    private final AccountStatementService statements;

    // NOTA: no podemos validar aquí que unitId pertenezca a orgId (esa data
    // vive en board-api), así que por ahora solo se valida que el usuario
    // tenga rol en el orgId indicado. Ver limitación de auth servicio-a-servicio
    // documentada en BoardApiClient.
    @GetMapping("/account-statement")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasRoleInOrg(authentication, #orgId, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'}) or
        @jwtAuth.isOwnUnit(authentication, #unitId)
    """)
    public AccountStatementResponse forUnit(@RequestParam String orgId, @RequestParam String unitId) {
        return statements.forUnit(unitId);
    }
}

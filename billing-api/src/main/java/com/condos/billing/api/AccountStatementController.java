package com.condos.billing.api;

import com.condos.billing.api.dto.AccountStatementResponse;
import com.condos.billing.service.AccountStatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

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

    @GetMapping(value = "/account-statement/export/csv", produces = "text/csv; charset=UTF-8")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasRoleInOrg(authentication, #orgId, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'}) or
        @jwtAuth.isOwnUnit(authentication, #unitId)
    """)
    public ResponseEntity<ByteArrayResource> exportCsv(@RequestParam String orgId, @RequestParam String unitId) {
        var st = statements.forUnit(unitId);
        byte[] bytes = statements.toCsv(st).getBytes(StandardCharsets.UTF_8);
        String filename = ("estado-cuenta-" + unitId + ".csv").replaceAll("[^A-Za-z0-9._-]", "_");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(bytes.length)
                .body(new ByteArrayResource(bytes));
    }
}

package com.condos.billing.api;

import com.condos.billing.api.dto.ChargeResponse;
import com.condos.billing.api.dto.CreateExtraordinaryChargeRequest;
import com.condos.billing.api.dto.GenerateChargesRequest;
import com.condos.billing.model.ChargeStatus;
import com.condos.billing.service.ChargeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/condos/api/billing")
@RequiredArgsConstructor
public class ChargeController {

    private final ChargeService charges;

    @PostMapping("/charges/generate")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasRoleInOrg(authentication, #orgId, {'ADMINISTRADOR','SUPERVISOR'})
    """)
    public List<ChargeResponse> generate(@RequestParam String orgId,
                                          @Valid @RequestBody GenerateChargesRequest req,
                                          HttpServletRequest http) {
        var created = charges.generateForPeriod(orgId, req.boardId(), req.period(), bearerToken(http));
        return created.stream().map(ChargeResponse::from).toList();
    }

    @PostMapping("/charges/extraordinary")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasRoleInOrg(authentication, #orgId, {'ADMINISTRADOR','SUPERVISOR'})
    """)
    public List<ChargeResponse> extraordinary(@RequestParam String orgId,
                                               @Valid @RequestBody CreateExtraordinaryChargeRequest req,
                                               HttpServletRequest http) {
        var created = charges.createExtraordinary(orgId, req, bearerToken(http));
        return created.stream().map(ChargeResponse::from).toList();
    }

    @GetMapping("/charges")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasRoleInOrg(authentication, #orgId, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})
    """)
    public Page<ChargeResponse> list(@RequestParam String orgId,
                                      @RequestParam(required = false) String unitId,
                                      @RequestParam(required = false) String boardId,
                                      @RequestParam(required = false) ChargeStatus status,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "50") int size,
                                      @RequestParam(defaultValue = "dueDate") String sortBy,
                                      @RequestParam(defaultValue = "DESC") Sort.Direction dir) {
        if (unitId == null && boardId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes indicar unitId o boardId");
        }
        var result = unitId != null
                ? charges.listByUnit(unitId, status, page, size, sortBy, dir)
                : charges.listByBoard(boardId, status, page, size, sortBy, dir);
        return result.map(ChargeResponse::from);
    }

    @GetMapping("/charges/{id}")
    @PreAuthorize("@jwtAuth.isSuperadmin(authentication) or @jwtAuth.hasAccessToCharge(authentication, #id, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})")
    public ChargeResponse get(@PathVariable String id) {
        var c = charges.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ChargeResponse.from(c);
    }

    private String bearerToken(HttpServletRequest http) {
        String h = http.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) return h.substring(7);
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing bearer token");
    }
}

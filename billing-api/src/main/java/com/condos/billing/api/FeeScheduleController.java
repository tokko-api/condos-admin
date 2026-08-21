package com.condos.billing.api;

import com.condos.billing.api.dto.CreateFeeScheduleRequest;
import com.condos.billing.api.dto.FeeScheduleResponse;
import com.condos.billing.api.dto.UpdateFeeScheduleRequest;
import com.condos.billing.service.FeeScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/condos/api/billing")
@RequiredArgsConstructor
public class FeeScheduleController {

    private final FeeScheduleService schedules;

    // NOTA: billing-api no tiene acceso a la BD de board-api, así que no puede
    // resolver boardId -> orgId por su cuenta (ver BoardApiClient). Por eso
    // orgId se recibe explícito aquí en vez de derivarse del board, hasta que
    // exista auth servicio-a-servicio real entre microservicios.
    @PostMapping("/boards/{boardId}/schedules")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasRoleInOrg(authentication, #orgId, {'ADMINISTRADOR','SUPERVISOR'})
    """)
    public FeeScheduleResponse create(@PathVariable String boardId,
                                       @RequestParam String orgId,
                                       @Valid @RequestBody CreateFeeScheduleRequest req,
                                       Authentication authentication) {
        var s = schedules.create(orgId, boardId, req.name(), req.amount(), req.currency(),
                req.frequency(), req.dueDayOfPeriod(), authentication.getName());
        return FeeScheduleResponse.from(s);
    }

    @GetMapping("/boards/{boardId}/schedules")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasRoleInOrg(authentication, #orgId, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})
    """)
    public Page<FeeScheduleResponse> list(@PathVariable String boardId,
                                           @RequestParam String orgId,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "50") int size,
                                           @RequestParam(defaultValue = "createdAt") String sortBy,
                                           @RequestParam(defaultValue = "DESC") Sort.Direction dir) {
        return schedules.list(boardId, page, size, sortBy, dir).map(FeeScheduleResponse::from);
    }

    @GetMapping("/schedules/{id}")
    @PreAuthorize("@jwtAuth.isSuperadmin(authentication) or @jwtAuth.hasAccessToSchedule(authentication, #id, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})")
    public FeeScheduleResponse get(@PathVariable String id) {
        var s = schedules.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return FeeScheduleResponse.from(s);
    }

    @PutMapping("/schedules/{id}")
    @PreAuthorize("@jwtAuth.isSuperadmin(authentication) or @jwtAuth.hasAccessToSchedule(authentication, #id, {'ADMINISTRADOR','SUPERVISOR'})")
    public FeeScheduleResponse update(@PathVariable String id, @RequestBody UpdateFeeScheduleRequest req) {
        var s = schedules.update(id, req.name(), req.amount(), req.currency(), req.frequency(),
                req.dueDayOfPeriod(), req.active());
        return FeeScheduleResponse.from(s);
    }
}

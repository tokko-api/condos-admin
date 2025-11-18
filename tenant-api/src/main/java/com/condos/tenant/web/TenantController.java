package com.condos.tenant.web;

import com.condos.tenant.model.TenantStatus;
import com.condos.tenant.service.TenantService;
import com.condos.tenant.web.dto.CreateTenantRequest;
import com.condos.tenant.web.dto.TenantResponse;
import com.condos.tenant.web.dto.UpdateTenantRequest;
import com.condos.tenant.web.mapper.TenantMapper;
import jakarta.validation.Valid;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/condos/api/tenant")
public class TenantController {

    private final TenantService service;

    public TenantController(TenantService service) {
        this.service = service;
    }

    // ======== CREATE ========
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SUPERADMIN')")
    public TenantResponse create(@Valid @RequestBody CreateTenantRequest req) {
        return TenantMapper.toDto(service.create(req));
    }

    // ======== READ ========
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPERADMIN','ADMIN@' + authentication.details)")
    public TenantResponse getById(@PathVariable("id") String id) {
        // delega en el servicio y mapea a DTO
        return TenantMapper.toDto(service.getById(id));
    }

    @GetMapping("/lookup")
    @PreAuthorize("""
  @jwtAuth.isSuperadmin(authentication) or
  @jwtAuth.hasRoleInAnyOrg(authentication, #ids, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})
""")
    public List<TenantResponse> getByIds(@RequestParam("ids") List<String> ids) {
        return service.getByIds(ids)
                .stream()
                .map(TenantMapper::toDto)
                .toList();
    }

    @GetMapping("/slug/{slug}")
    @PreAuthorize("hasAnyAuthority('SUPERADMIN','ADMIN@' + authentication.details)")
    public TenantResponse getBySlug(@PathVariable String slug) {
        return TenantMapper.toDto(service.getBySlugActive(slug));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SUPERADMIN')")
    public Page<TenantResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction dir,
            @RequestParam(defaultValue = "false") boolean includeArchived
    ) {
        // Por defecto ocultamos ARCHIVED; si piden incluirlos, usa el listado completo
        var result = includeArchived
                ? service.list(q, page, size, sortBy, dir)        // incluye todos
                : service.listActive(q, page, size, sortBy, dir);  // sólo ACTIVE/SUSPENDED
        return result.map(TenantMapper::toDto);
    }

    // ======== UPDATE ========
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPERADMIN')")
    public TenantResponse update(@PathVariable String id, @Valid @RequestBody UpdateTenantRequest req) {
        return TenantMapper.toDto(service.update(id, req));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPERADMIN')")
    public TenantResponse patch(@PathVariable String id, @RequestBody UpdateTenantRequest req) {
        return TenantMapper.toDto(service.update(id, req));
    }

    // ======== SOFT DELETE (ARCHIVE) ========
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('SUPERADMIN')")
    public void archive(@PathVariable String id, Authentication auth,
                        @RequestParam(required = false) String reason) {
        var userId = auth != null ? auth.getName() : "system";
        service.archive(id, userId, reason);
    }

    // ======== CAMBIO DE STATUS ========
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SUPERADMIN')")
    public Map<String, Object> changeStatus(@PathVariable String id,
                                            @RequestParam TenantStatus status,
                                            Authentication auth) {
        switch (status) {
            case ACTIVE -> service.activate(id);
            case SUSPENDED -> service.suspend(id);
            case ARCHIVED -> service.archive(id, auth != null ? auth.getName() : "system", "manual");
        }
        return Map.of("id", id, "status", status.name());
    }

    // ======== HANDLERS DE ERRORES ========
    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(BAD_REQUEST)
    public Map<String, Object> onDup(DuplicateKeyException ex) {
        return Map.of("error", "duplicate", "message", ex.getMessage());
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> onNotFound(RuntimeException ex) {
        return Map.of("error", "not_found", "message", ex.getMessage());
    }
}
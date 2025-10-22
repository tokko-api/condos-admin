package com.condos.user.api;

import com.condos.user.dto.CreateUserRequest;
import com.condos.user.security.JwtAuth;
import com.condos.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static com.condos.user.api.Dtos.*;

@RestController
@RequestMapping("/condos/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;
    private final JwtAuth jwtAuth;

    @PostMapping("/users")
    @PreAuthorize("""
    @jwtAuth.isSuperadmin(authentication) or
    (
      @jwtAuth.hasRoleInOrg(authentication, #req.orgId, {'ADMINISTRADOR'})
      and (#req.role.name() == 'SUPERVISOR' or #req.role.name() == 'OPERATIVO')
    )
""")
    public UserSummary create(org.springframework.security.core.Authentication authentication,
                              @RequestBody CreateUserRequest req) {
        // Ya NO necesitas validar strings del rol: viene tipado como enum.
        // La lógica de quién puede crear a quién ya está en @PreAuthorize.
        return service.create(authentication, req);
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ROLE_USER')") // token válido
    public List<UserSummary> list(@RequestParam(required = false) String orgId,
                                  @RequestParam(required = false) String role,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) String q) {
        return service.list(orgId, role, status, q);
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public UserSummary get(@PathVariable String id) {
        return service.get(id);
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("@jwtAuth.canManage(authentication, #orgId, #targetRole)")
    public UserSummary update(@PathVariable String id, @RequestBody UpdateUserRequest req) {
        return service.update(id, req);
    }

    @PutMapping("/users/{id}/orgs/{orgId}/role")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
       @PreAuthorize("@jwtAuth.canManage(authentication, #orgId, #targetRole)")
    """)
    public UserSummary changeRole(@PathVariable String id, @PathVariable String orgId,
                                  @RequestBody ChangeRoleRequest req) {
        // Si es ADMIN, solo puede asignar SUPERVISOR u OPERATIVO
        if (!"SUPERADMIN".equals(req.role()) && !"ADMINISTRADOR".equals(req.role())
                && !"SUPERVISOR".equals(req.role()) && !"OPERATIVO".equals(req.role())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "invalid role");
        }
        return service.changeRole(id, orgId, req.role());
    }

    @PatchMapping("/users/{id}/orgs/{orgId}/status")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @PreAuthorize("@jwtAuth.canManage(authentication, #orgId, #targetRole)")
    """)
    public UserSummary changeStatus(@PathVariable String id, @PathVariable String orgId,
                                    @RequestBody ChangeStatusRequest req) {
        return service.changeStatus(id, orgId, req.status());
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("@jwtAuth.canManage(authentication, #orgId, #targetRole)")
    public void delete(@PathVariable String id) {
        service.delete(id);
    }

    @PatchMapping("/users/{id}/org")
    @PreAuthorize("@jwtAuth.isSuperadmin(authentication)")
    public UserSummary changeOrg(@PathVariable String id, @RequestBody ChangeOrgRequest req) {
        return service.changeOrg(id, req.orgId());
    }


}
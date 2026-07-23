package com.condos.user.api;

import com.condos.user.dto.CreateUserRequest;
import com.condos.user.security.JwtAuth;
import com.condos.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static com.condos.user.api.Dtos.*;

@RestController
@RequestMapping("/condos/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

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

    @RequestMapping(value = "/users/{id}/orgs/{orgId}/role", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @PreAuthorize("@jwtAuth.isSuperadmin(authentication) or @jwtAuth.canManage(authentication, #orgId, #req.role())")
    public UserSummary changeRole(@PathVariable String id, @PathVariable String orgId,
                                  @RequestBody ChangeRoleRequest req) {
        // Validate allowed roles
        if (!"SUPERADMIN".equals(req.role()) && !"ADMINISTRADOR".equals(req.role())
                && !"SUPERVISOR".equals(req.role()) && !"OPERATIVO".equals(req.role())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "invalid role");
        }
        return service.changeRole(id, orgId, req.role());
    }


    @PatchMapping(value = "/users/{id}/orgs/{orgId}/status")
    @PreAuthorize("@jwtAuth.isSuperadmin(authentication) or @jwtAuth.canManage(authentication, #orgId)")
    public UserSummary changeStatus(@PathVariable String id,
                                    @PathVariable String orgId,
                                    @RequestBody ChangeStatusRequest req,
                                    Authentication auth) {
        return service.changeStatus(id, orgId, req.status());
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("@jwtAuth.canManage(authentication, #orgId, #targetRole)")
    public  ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{id}/org")
    @PreAuthorize("@jwtAuth.isSuperadmin(authentication)")
    public UserSummary changeOrg(@PathVariable String id, @RequestBody ChangeOrgRequest req) {
        return service.changeOrg(id, req.orgId());
    }


}
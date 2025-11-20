package com.condos.auth.api;

import com.condos.auth.accounts.AuthAccountRepository;
import com.condos.shared.security.JwtService;
import com.condos.shared.security.JwtService.OrgRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/condos/api/auth")
@Tag(name = "Auth", description = "Endpoints de autenticación")
public class AuthController {

    private final AuthAccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;
    private final UserApiClient userApi;

    public AuthController(AuthAccountRepository accounts,
                          PasswordEncoder passwordEncoder,
                          JwtService jwt,
                          UserApiClient userApi) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
        this.userApi = userApi;
    }

    // ===== DTOs =====
    public record LoginReq(String email, String password, String orgId) {}
    public record PublicUser(String id, String email) {}
    public record AuthRes(String token, String orgId, List<String> roles, PublicUser user) {}

    // ===== Endpoints =====

    @Operation(summary = "Login", description = "Autentica y devuelve un JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    })
    @PostMapping("/login")
    public AuthRes login(@RequestBody LoginReq r) {
        final String email = normEmail(r.email());
        final var acc = accounts.findByEmail(email).orElse(null);
        if (acc == null || acc.passwordHash == null || !passwordEncoder.matches(r.password(), acc.passwordHash)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials");
        }

        // 1) Traer asignaciones por EMAIL (Opción A)
        final var assignments = userApi
                .getAssignmentsByEmail(email)         // <-- usa el nuevo cliente por email
                .block(Duration.ofSeconds(3));        // bloquea corto (si estás en MVC); si todo es WebFlux, vuelve el método Mono<AuthRes>

        if (assignments == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "user-api unavailable");
        }

        // 2) ¿Es superadmin?
        final boolean isSuper = assignments.stream()
                .anyMatch(a -> "SUPERADMIN".equalsIgnoreCase(a.role()));

        // 3) Filtrar sólo ORGs ACTIVAS y mapear a OrgRole(orgId, role)
        final var activeOrgs = assignments.stream()
                .filter(a -> "ACTIVE".equalsIgnoreCase(a.status()))
                .map(a -> new OrgRole(a.orgId(), a.role()))
                .collect(Collectors.toList());

        // 4) Resolver orgId destino
        final String requestedOrg = norm(r.orgId());
        final String orgId = resolveOrgId(requestedOrg, isSuper, activeOrgs);

        // 5) Roles efectivos para esa org (o SUPERADMIN)
        final var rolesForOrg = rolesForOrg(orgId, isSuper, activeOrgs);
        if (!isSuper && (orgId == null || rolesForOrg.isEmpty())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "user has no active roles in org " + orgId);
        }

        // 6) Construir JWT: subject = acc.id, claims = email + orgs (id/role), ver = accountVersion
        final long version = acc.accountVersion == null ? 1L : acc.accountVersion;
        final String token = jwt.generate(acc.id, acc.email, activeOrgs, version);

        return new AuthRes(token, orgId, rolesForOrg, new PublicUser(acc.id, acc.email));
    }

    @GetMapping("/ping")
    public Map<String,String> ping() { return Map.of("ok","true"); }

    @Operation(summary = "Quién soy", security = { @SecurityRequirement(name = "bearerAuth") })
    @GetMapping("/me")
    public Map<String,Object> me(@RequestHeader("Authorization") String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        var jws = jwt.parse(authorization.substring(7));
        String userId = jws.getBody().getSubject();
        String email  = (String) jws.getBody().get("email");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> orgsFromToken = (List<Map<String, String>>) jws.getBody().get("orgs");
        Long ver = ((Number) jws.getBody().get("ver")).longValue();

        // --- Enriquecer con nombres de colonia ---
        Map<String, String> nameByOrg = new HashMap<>();
        try {
            // Asegúrate de que UserApiClient devuelva también orgName
            var assignments = userApi.getAssignmentsByEmail(email).block(Duration.ofSeconds(3));
            if (assignments != null) {
                assignments.forEach(a -> {
                    String id = a.orgId();
                    String nm = a.orgName() != null && !a.orgName().isBlank() ? a.orgName() : id;
                    nameByOrg.put(id, nm);
                });
            }
        } catch (Exception ignored) {
            // si falla el user-api, devolvemos los IDs como fallback
        }

        List<Map<String, String>> orgs = orgsFromToken.stream()
                .map(o -> {
                    String id   = o.get("orgId");
                    String role = o.get("role");
                    String name = nameByOrg.getOrDefault(id, id);
                    return Map.of("orgId", id, "name", name, "role", role);
                })
                .toList();

        return Map.of(
                "id", userId,
                "email", email,
                "orgs", orgs,
                "ver", ver
        );
    }

    // ===== Helpers =====

    private static String norm(String s) {
        return s == null ? null : s.trim().toLowerCase(Locale.ROOT);
    }

    private static String normEmail(String s) {
        return s == null ? null : s.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveOrgId(String requestedOrg, boolean isSuper, List<OrgRole> activeOrgs) {
        if (requestedOrg != null && !requestedOrg.isBlank()) {
            // si no es super, valida que requestedOrg esté entre sus activas
            if (!isSuper && activeOrgs.stream().noneMatch(o -> requestedOrg.equals(o.orgId()))) {
                return null;
            }
            return requestedOrg;
        }
        if (isSuper) {
            if (!activeOrgs.isEmpty()) return activeOrgs.get(0).orgId();
            // fallback superadmin sin orgs activas: usa el root fijo que ya tienes en DB
            return "000000000000000000000000";
        }
        if (activeOrgs.size() == 1) return activeOrgs.get(0).orgId();
        return null;
    }

    private List<String> rolesForOrg(String orgId, boolean isSuper, List<OrgRole> activeOrgs) {
        if (isSuper) return List.of("SUPERADMIN");
        if (orgId == null) return List.of();
        return activeOrgs.stream()
                .filter(o -> orgId.equals(o.orgId()))
                .map(OrgRole::role)
                .toList();
    }

    public record ChangePasswordReq(String newPassword) {}
    public record ChangeEmailReq(String newEmail) {}

    @PatchMapping("/users/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String id,
            @RequestBody ChangePasswordReq req
    ) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        var jws = jwt.parse(authorization.substring(7));

        @SuppressWarnings("unchecked")
        List<Map<String, String>> requesterOrgs =
                (List<Map<String, String>>) jws.getBody().get("orgs");

        boolean isSuper = requesterOrgs.stream()
                .anyMatch(o -> "SUPERADMIN".equalsIgnoreCase(o.get("role")));

        boolean isAdmin = requesterOrgs.stream()
                .anyMatch(o -> "ADMINISTRADOR".equalsIgnoreCase(o.get("role")));

        // ===== Si no es super ni admin → prohibido =====
        if (!isSuper && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not allowed");
        }

        // ===== Si es admin, verificar que el target pertenece a alguna de sus orgs =====
        if (!isSuper) {
            var targetAssignments = userApi.getAssignmentsByUserId(id).block();
            boolean sameOrg = targetAssignments.stream().anyMatch(ta ->
                    requesterOrgs.stream().anyMatch(ro ->
                            ro.get("orgId").equals(ta.orgId())
                    )
            );
            if (!sameOrg) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not allowed (different org)");
            }
        }

        // ===== Validar password =====
        if (req.newPassword() == null || req.newPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password required");
        }

        var acc = accounts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "account not found"));

        acc.passwordHash = passwordEncoder.encode(req.newPassword().trim());
        acc.accountVersion = acc.accountVersion == null ? 1L : acc.accountVersion + 1;

        accounts.save(acc);
    }
    @PatchMapping("/users/{id}/email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeEmail(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String id,
            @RequestBody ChangeEmailReq req
    ) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        var jws = jwt.parse(authorization.substring(7));
        @SuppressWarnings("unchecked")
        List<Map<String, String>> requesterOrgs =
                (List<Map<String, String>>) jws.getBody().get("orgs");

        boolean isSuper = requesterOrgs.stream()
                .anyMatch(o -> "SUPERADMIN".equalsIgnoreCase(o.get("role")));

        boolean isAdmin = requesterOrgs.stream()
                .anyMatch(o -> "ADMINISTRADOR".equalsIgnoreCase(o.get("role")));

        // SUPERADMIN puede todo
        if (!isSuper) {
            if (!isAdmin) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not allowed");
            }

            // ADMIN solo si el target pertenece a su org
            var targetAssignments = userApi.getAssignmentsByUserId(id).block();
            boolean sameOrg = targetAssignments.stream().anyMatch(ta ->
                    requesterOrgs.stream().anyMatch(ro ->
                            ro.get("orgId").equals(ta.orgId())
                    )
            );
            if (!sameOrg) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not allowed (different org)");
            }
        }

        if (req.newEmail() == null || req.newEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email required");
        }

        String normalized = normEmail(req.newEmail());

        accounts.findByEmail(normalized).ifPresent(existing -> {
            if (!existing.id.equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "email already in use");
            }
        });

        var acc = accounts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "account not found"));

        acc.email = normalized;
        accounts.save(acc);
    }
}
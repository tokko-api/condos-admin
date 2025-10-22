package com.condos.user.service;

import com.condos.user.api.Dtos.*;
import com.condos.user.domain.UserProfile;
import com.condos.user.domain.UserProfileRepository;
import com.condos.user.dto.CreateUserRequest;
import com.condos.user.model.UserRole;
import com.condos.user.model.UserStatus;
import com.condos.user.util.ObjectIds;
import com.condos.user.util.Passwords;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@Service
public class UserService {

    private final UserProfileRepository repo;
    private final AuthApiClient authApiClient;

    public UserService(UserProfileRepository repo, AuthApiClient authApiClient) {
        this.repo = repo;
        this.authApiClient = authApiClient;
    }

    public UserSummary create(Authentication auth, CreateUserRequest req) {
        // 0) normalizar email
        final String email = req.email() == null ? null : req.email().trim().toLowerCase();

        // 1) obtener accountId desde auth-api
        final String accountId;
        if (req.provisionAccount()) {
            final String pwd = (req.tempPassword() == null || req.tempPassword().isBlank())
                    ? Passwords.generateSecure(12)
                    : req.tempPassword();
            accountId = authApiClient.provisionAccount(email, pwd);
        } else {
            accountId = authApiClient.findAccountIdByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "auth_account not found for email; set provisionAccount=true or create it first"));
        }

        // 2) upsert del user_profile usando el MISMO id de auth_account
        UserProfile profile = repo.findById(accountId)
                .orElseGet(() -> UserProfile.builder()
                        .id(accountId)                  // String
                        .email(email)
                        .fullName(req.fullName())
                        .orgAssignments(new ArrayList<>())
                        .build());

        // 3) agregar/actualizar assignment (orgId como ObjectId)
        ObjectId orgObjectId = ObjectIds.parse(req.orgId()); // lanza 400 si inválido

        var existing = profile.getOrgAssignments().stream()
                .filter(a -> Objects.equals(a.getOrgId(), orgObjectId))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().setRole(req.role());               // enum UserRole
            existing.get().setStatus(UserStatus.ACTIVE);      // enum
        } else {
            profile.getOrgAssignments().add(UserProfile.OrgAssignment.builder()
                    .orgId(orgObjectId)
                    .role(req.role())
                    .status(UserStatus.ACTIVE)
                    .build());
        }

        // 4) guardar y devolver resumen
        repo.save(profile);
        return toSummary(profile);
    }

    private UserSummary toSummary(UserProfile up) {
        var orgs = up.orgAssignments.stream()
                .map(a -> new UserSummary.Org(
                        a.orgId != null ? a.orgId.toHexString() : null,
                        a.role != null ? a.role.name() : null,
                        a.status != null ? a.status.name() : null
                ))
                .toList();

        return new UserSummary(
                up.id,
                up.fullName,
                up.email,
                orgs
        );
    }

    public UserSummary get(String id) {
        var up = repo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        return toSummary(up);
    }

    public List<UserSummary> list(String orgIdStr, String roleStr, String statusStr, String q) {
        var all = (q != null && !q.isBlank())
                ? repo.findByEmailContainingIgnoreCase(q)
                : repo.findAll();

        // preparar tipos reales
        ObjectId tmpOrgId = null;
        if (orgIdStr != null && !orgIdStr.isBlank()) {
            try { tmpOrgId = new ObjectId(orgIdStr); }
            catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid orgId");
            }
        }
        UserRole tmpRole   = (roleStr   == null || roleStr.isBlank())   ? null : UserRole.valueOf(roleStr);
        UserStatus tmpStat = (statusStr == null || statusStr.isBlank()) ? null : UserStatus.valueOf(statusStr);

        // copias finales para las lambdas
        final ObjectId fOrgId = tmpOrgId;
        final UserRole fRole = tmpRole;
        final UserStatus fStatus = tmpStat;

        return all.stream()
                .filter(u -> fOrgId == null  || u.getOrgAssignments().stream().anyMatch(a -> fOrgId.equals(a.getOrgId())))
                .filter(u -> fRole == null   || u.getOrgAssignments().stream().anyMatch(a -> fRole.equals(a.getRole())))
                .filter(u -> fStatus == null || u.getOrgAssignments().stream().anyMatch(a -> fStatus.equals(a.getStatus())))
                .map(this::toSummary)
                .toList();
    }
    public UserSummary update(String id, UpdateUserRequest req) {
        var up = repo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (req.fullName() != null) up.fullName = req.fullName();
        if (req.email() != null)    up.email = req.email();
        repo.save(up);
        return toSummary(up);
    }

    public UserSummary changeRole(String id, String orgId, String role) {
        var up = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ObjectId orgObjectId;
        try {
            orgObjectId = new ObjectId(orgId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid orgId: " + orgId);
        }

        var assignment = up.orgAssignments.stream()
                .filter(x -> orgObjectId.equals(x.orgId))
                .findFirst()
                .orElseGet(() -> {
                    var na = new UserProfile.OrgAssignment();
                    na.orgId = orgObjectId;
                    na.status = UserStatus.ACTIVE;
                    up.orgAssignments.add(na);
                    return na;
                });

        try {
            assignment.role = UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + role);
        }

        repo.save(up);
        return toSummary(up);
    }


    public UserSummary changeStatus(String id, String orgId, String status) {
        var up = repo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        var a = up.orgAssignments.stream().filter(x -> orgId.equals(x.orgId)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "no assignment for org"));
        a.status = UserStatus.SUSPENDED;
        repo.save(up);
        return toSummary(up);
    }

    public void delete(String id) {
        // Puedes hacer soft-delete marcando INACTIVE en todas sus orgs.
        var up = repo.findById(id).orElse(null);
        if (up != null) {
            up.orgAssignments.forEach(a -> a.status = UserStatus.ARCHIVED);
            repo.save(up);
        }
    }


    public UserSummary changeOrg(String id, String newOrgId) {
        var up = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (up.orgAssignments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User has no org assignments");
        }

        ObjectId orgObjectId;
        try {
            orgObjectId = new ObjectId(newOrgId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid orgId: " + newOrgId);
        }

        up.orgAssignments.forEach(a -> a.orgId = orgObjectId);

        repo.save(up);
        return toSummary(up);
    }
}
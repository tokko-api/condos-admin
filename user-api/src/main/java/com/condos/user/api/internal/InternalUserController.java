package com.condos.user.api.internal;

import com.condos.user.domain.UserProfile;
import com.condos.user.domain.UserProfileRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/condos/api/user/internal/users")
public class InternalUserController {

    private final UserProfileRepository repo;

    public InternalUserController(UserProfileRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/{id}/assignments")
    public List<OrgAssignmentDto> getAssignments(@PathVariable String id) {
        var profile = repo.findById(id).orElse(null);
        if (profile == null) {
            return List.of();
        }
        return profile.getOrgAssignments().stream()
                .map(a -> new OrgAssignmentDto(
                        a.getOrgId().toHexString(),   // ObjectId → String
                        a.getRole().name(),           // UserRole → String
                        a.getStatus().name()          // UserStatus → String
                ))
                .toList();
    }

    @GetMapping("/by-email/{email}/assignments")
    public List<OrgAssignmentDto> getAssignmentsByEmail(@PathVariable String email) {
        var profile = repo.findByEmail(email.trim().toLowerCase()).orElse(null);
        if (profile == null) return List.of();
        return profile.getOrgAssignments().stream().map(OrgAssignmentDto::new).toList();
    }

    public record OrgAssignmentDto(String orgId, String role, String status) {
        public OrgAssignmentDto(UserProfile.OrgAssignment a) {
            this(
                    a.getOrgId() != null ? a.getOrgId().toHexString() : null,
                    a.getRole() != null ? a.getRole().name() : null,
                    a.getStatus() != null ? a.getStatus().name() : null
            );
        }
    }
}
package com.condos.user.api;

import com.condos.user.model.UserStatus;

import java.util.List;

public class Dtos {
    public record UpdateUserRequest(String fullName, String email) {}
    public record ChangeRoleRequest(String role) {}
    public record ChangeStatusRequest(UserStatus status) {}       // ACTIVE | INACTIVE
    public record UserSummary(String id, String fullName, String email, List<Org> orgs) {
        public record Org(String orgId, String role, String status) {}
    }
    public record ChangeOrgRequest(String orgId) {}
}
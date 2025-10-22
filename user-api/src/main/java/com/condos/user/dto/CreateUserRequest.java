package com.condos.user.dto;

import com.condos.user.model.UserRole;

public record CreateUserRequest(
        String email,
        String fullName,
        String orgId,
        UserRole role,              // ADMINISTRADOR | SUPERVISOR | OPERATIVO | SUPERADMIN
        boolean provisionAccount, // true => crear en auth_account
        String tempPassword       // opcional (si null, genera uno)
) {}
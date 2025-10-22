package com.condos.user.model;

public enum UserRole {
    SUPERADMIN,
    ADMINISTRADOR,
    SUPERVISOR,
    OPERATIVO;

    public static boolean canManage(UserRole actor, UserRole target) {
        if (actor == SUPERADMIN) return true;
        if (actor == ADMINISTRADOR) {
            return target == SUPERVISOR || target == OPERATIVO;
        }
        return false;
    }
}
package com.condos.user.model;

public enum UserRole {
    SUPERADMIN,
    ADMINISTRADOR,
    SUPERVISOR,
    OPERATIVO,
    CONDOMINO;

    public static boolean canManage(UserRole actor, UserRole target) {
        if (actor == SUPERADMIN) return true;
        if (actor == ADMINISTRADOR) {
            return target == SUPERVISOR || target == OPERATIVO || target == CONDOMINO;
        }
        if (actor == SUPERVISOR) {
            return target == CONDOMINO;
        }
        return false;
    }
}
package com.condos.billing.service;

import java.util.List;

/**
 * Cliente hacia board-api para resolver las unidades activas de una colonia
 * al generar cargos.
 *
 * LIMITACIÓN CONOCIDA: esta llamada usa el JWT del usuario que hizo la
 * petición original (se lo pasamos tal cual), NO un token de servicio. Eso
 * funciona mientras quien genera cargos tenga permiso para listar unidades
 * de ese board en board-api, pero no es la auth servicio-a-servicio "real"
 * que se documentó como pendiente en el diseño (la misma que falta en
 * condos-user-api -> condos-auth-api). Cuando se resuelva esa pieza de
 * infraestructura compartida, esta clase debe cambiar a usar un token de
 * servicio en vez de reenviar el del usuario.
 */
public interface BoardApiClient {
    record UnitRef(String id, String identifier, String residentUserId) {}

    List<UnitRef> listActiveUnitIds(String boardId, String bearerToken);

    /** Resuelve una unidad puntual (usado para validar ownership de un condomino). */
    UnitRef getUnit(String unitId, String bearerToken);
}

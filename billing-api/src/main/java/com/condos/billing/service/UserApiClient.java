package com.condos.billing.service;

/**
 * Cliente hacia user-api para resolver el correo de un condómino a partir
 * de su residentUserId (necesario para enviar recordatorios de pago).
 *
 * Misma limitación conocida que BoardApiClient: reenvía el JWT del usuario
 * que hizo la petición original en lugar de un token de servicio.
 */
public interface UserApiClient {
    record UserRef(String id, String email, String fullName) {}

    UserRef getUser(String userId, String bearerToken);
}

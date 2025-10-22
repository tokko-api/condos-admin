package com.condos.shared.security;

import java.util.List;

/**
 * Payload estándar que meterás en el JWT.
 */
public record JwtPayload(String sub, String orgId, List<String> roles) {}
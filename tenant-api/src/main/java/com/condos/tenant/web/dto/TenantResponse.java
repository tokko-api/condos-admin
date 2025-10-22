package com.condos.tenant.web.dto;

import com.condos.tenant.model.TenantStatus;

import java.time.Instant;
import java.util.Map;

public record TenantResponse(
        String id,
        String name,
        String slug,
        String email,
        String phone,
        String address,
        Map<String, Object> settings,
        Instant createdAt,
        Instant updatedAt,
        TenantStatus status
) {}
package com.condos.tenant.web.mapper;

import com.condos.tenant.model.Tenant;
import com.condos.tenant.web.dto.TenantResponse;

public final class TenantMapper {
    private TenantMapper() {}

    public static TenantResponse toDto(Tenant t) {
        return new TenantResponse(
                t.id != null ? t.id.toHexString() : null, t.name, t.slug, t.email, t.phone,
                t.address, t.settings, t.createdAt, t.updatedAt,t.status
        );
    }
}
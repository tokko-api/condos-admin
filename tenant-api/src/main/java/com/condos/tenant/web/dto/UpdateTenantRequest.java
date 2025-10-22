package com.condos.tenant.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record UpdateTenantRequest(
        @Size(max = 80) String name,
        @Pattern(regexp = "^[a-z0-9-]{3,40}$") String slug,
        @Email @Size(max = 120) String email,
        @Size(max = 30) String phone,
        @Size(max = 200) String address,
        Map<String, Object> settings
) {}
package com.condos.billing.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BulkReconcileRequest(
        @NotEmpty List<String> paymentIds,
        @NotNull Boolean approve
) {}

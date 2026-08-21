package com.condos.billing.api.dto;

import jakarta.validation.constraints.NotNull;

public record ReconcilePaymentRequest(
        @NotNull Boolean approve // true = RECONCILED, false = REJECTED
) {}

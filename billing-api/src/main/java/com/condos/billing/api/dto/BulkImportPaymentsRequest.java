package com.condos.billing.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkImportPaymentsRequest(
        @NotEmpty @Size(max = 500) List<BulkImportPaymentRow> rows
) {}

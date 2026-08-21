package com.condos.billing.api.dto;

import com.condos.billing.model.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Una fila del Excel/CSV: identificador de unidad tal cual aparece en board-api ("Casa 12"). */
public record BulkImportPaymentRow(
        @NotBlank String unitIdentifier,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        PaymentMethod method // opcional, default TRANSFER
) {}

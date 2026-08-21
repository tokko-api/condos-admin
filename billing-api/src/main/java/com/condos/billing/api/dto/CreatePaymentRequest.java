package com.condos.billing.api.dto;

import com.condos.billing.model.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreatePaymentRequest(
        @NotBlank String unitId,
        @NotEmpty List<String> chargeIds,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull PaymentMethod method,
        String receiptFileId
) {}

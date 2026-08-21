package com.condos.billing.api.dto;

import com.condos.billing.model.FeeFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateFeeScheduleRequest(
        @NotBlank String name,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        String currency, // default "MXN" si viene vacío
        @NotNull FeeFrequency frequency,
        @Min(1) @Max(28) Integer dueDayOfPeriod
) {}

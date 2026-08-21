package com.condos.billing.api.dto;

import com.condos.billing.model.FeeFrequency;

import java.math.BigDecimal;

public record UpdateFeeScheduleRequest(
        String name,
        BigDecimal amount,
        String currency,
        FeeFrequency frequency,
        Integer dueDayOfPeriod,
        Boolean active
) {}

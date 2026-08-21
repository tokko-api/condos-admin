package com.condos.billing.api.dto;

import com.condos.billing.model.FeeFrequency;
import com.condos.billing.model.FeeSchedule;

import java.math.BigDecimal;
import java.time.Instant;

public record FeeScheduleResponse(
        String id,
        String boardId,
        String orgId,
        String name,
        BigDecimal amount,
        String currency,
        FeeFrequency frequency,
        int dueDayOfPeriod,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        String createdBy
) {
    public static FeeScheduleResponse from(FeeSchedule s) {
        return new FeeScheduleResponse(
                s.getId(), s.getBoardId(), s.getOrgId(), s.getName(), s.getAmount(), s.getCurrency(),
                s.getFrequency(), s.getDueDayOfPeriod(), s.isActive(), s.getCreatedAt(), s.getUpdatedAt(),
                s.getCreatedBy()
        );
    }
}

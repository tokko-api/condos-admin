package com.condos.billing.api.dto;

import com.condos.billing.model.Charge;
import com.condos.billing.model.ChargeStatus;
import com.condos.billing.model.ChargeType;

import java.math.BigDecimal;
import java.time.Instant;

public record ChargeResponse(
        String id,
        String boardId,
        String orgId,
        String unitId,
        String feeScheduleId,
        ChargeType type,
        String concept,
        BigDecimal amount,
        String period,
        Instant dueDate,
        ChargeStatus status,
        String assemblyActaNumber,
        Instant createdAt,
        Instant updatedAt
) {
    public static ChargeResponse from(Charge c) {
        return new ChargeResponse(
                c.getId(), c.getBoardId(), c.getOrgId(), c.getUnitId(), c.getFeeScheduleId(),
                c.getType(), c.getConcept(), c.getAmount(), c.getPeriod(), c.getDueDate(), c.getStatus(),
                c.getAssemblyRef() != null ? c.getAssemblyRef().getActaNumber() : null,
                c.getCreatedAt(), c.getUpdatedAt()
        );
    }
}

package com.condos.board.api.dto;

import com.condos.board.model.Unit;
import com.condos.board.model.UnitStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record UnitResponse(
        String id,
        String boardId,
        String orgId,
        String identifier,
        String ownerName,
        String residentUserId,
        BigDecimal coefficient,
        UnitStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static UnitResponse from(Unit u) {
        return new UnitResponse(
                u.getId(), u.getBoardId(), u.getOrgId(), u.getIdentifier(),
                u.getOwnerName(), u.getResidentUserId(), u.getCoefficient(),
                u.getStatus(), u.getCreatedAt(), u.getUpdatedAt()
        );
    }
}

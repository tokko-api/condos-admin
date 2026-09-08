package com.condos.board.api.dto;

import java.math.BigDecimal;

public record UpdateUnitRequest(
        String identifier,
        String ownerName,
        String residentUserId,
        BigDecimal coefficient,
        Boolean committeeMember
) {}

package com.condos.board.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateUnitRequest(
        @NotBlank String identifier,   // "Casa 12", "Depto 4B"
        String ownerName,
        String residentUserId,
        BigDecimal coefficient
) {}

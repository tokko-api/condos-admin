package com.condos.board.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdateReservationRequest(
        @NotNull LocalDate date,
        Integer peopleCount,
        String note
) {}

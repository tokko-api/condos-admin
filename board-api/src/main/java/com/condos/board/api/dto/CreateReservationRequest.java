package com.condos.board.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateReservationRequest(
        @NotBlank String unitId,
        @NotNull LocalDate date,
        String startTime, // "HH:mm", requerido solo si la amenidad usa horarios por bloque
        Integer peopleCount,
        String note
) {}

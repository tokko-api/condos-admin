package com.condos.board.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAmenityRequest(
        @NotBlank String name,
        String description,
        Integer maxPeoplePerReservation,
        Integer maxReservationsPerUnitPerDay,
        Integer maxReservationsPerDay,
        Integer advanceBookingDays,
        String notes
) {}

package com.condos.board.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateAmenityRequest(
        @NotBlank String name,
        String description,
        Integer maxPeoplePerReservation,
        Integer maxReservationsPerUnitPerDay,
        Integer maxReservationsPerDay,
        Integer advanceBookingDays,
        String openTime,
        String closeTime,
        Integer slotDurationMinutes,
        List<BlockedDateDto> blockedDates,
        List<RecurringBlockDto> recurringBlocks,
        String notes
) {}

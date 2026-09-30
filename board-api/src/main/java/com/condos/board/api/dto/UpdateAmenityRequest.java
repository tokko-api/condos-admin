package com.condos.board.api.dto;

import java.util.List;

public record UpdateAmenityRequest(
        String name,
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

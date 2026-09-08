package com.condos.board.api.dto;

public record UpdateAmenityRequest(
        String name,
        String description,
        Integer maxPeoplePerReservation,
        Integer maxReservationsPerUnitPerDay,
        Integer maxReservationsPerDay,
        Integer advanceBookingDays,
        String notes
) {}

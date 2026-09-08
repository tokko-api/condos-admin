package com.condos.board.api.dto;

import com.condos.board.model.Amenity;
import com.condos.board.model.AmenityStatus;

import java.time.Instant;

public record AmenityResponse(
        String id,
        String boardId,
        String orgId,
        String name,
        String description,
        Integer maxPeoplePerReservation,
        Integer maxReservationsPerUnitPerDay,
        Integer maxReservationsPerDay,
        Integer advanceBookingDays,
        String notes,
        AmenityStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static AmenityResponse from(Amenity a) {
        return new AmenityResponse(
                a.getId(), a.getBoardId(), a.getOrgId(), a.getName(), a.getDescription(),
                a.getMaxPeoplePerReservation(), a.getMaxReservationsPerUnitPerDay(),
                a.getMaxReservationsPerDay(), a.getAdvanceBookingDays(), a.getNotes(),
                a.getStatus(), a.getCreatedAt(), a.getUpdatedAt()
        );
    }
}

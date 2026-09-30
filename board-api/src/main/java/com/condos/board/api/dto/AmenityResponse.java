package com.condos.board.api.dto;

import com.condos.board.model.Amenity;
import com.condos.board.model.AmenityStatus;

import java.time.Instant;

import java.util.List;

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
        String openTime,
        String closeTime,
        Integer slotDurationMinutes,
        List<String> timeSlots, // horas de inicio generadas, vacío si no usa horarios
        List<BlockedDateDto> blockedDates,
        List<RecurringBlockDto> recurringBlocks,
        String notes,
        AmenityStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static AmenityResponse from(Amenity a) {
        return new AmenityResponse(
                a.getId(), a.getBoardId(), a.getOrgId(), a.getName(), a.getDescription(),
                a.getMaxPeoplePerReservation(), a.getMaxReservationsPerUnitPerDay(),
                a.getMaxReservationsPerDay(), a.getAdvanceBookingDays(),
                a.getOpenTime(), a.getCloseTime(), a.getSlotDurationMinutes(), a.generateSlots(),
                BlockedDateDto.fromModelList(a.getBlockedDates()),
                RecurringBlockDto.fromModelList(a.getRecurringBlocks()),
                a.getNotes(), a.getStatus(), a.getCreatedAt(), a.getUpdatedAt()
        );
    }
}

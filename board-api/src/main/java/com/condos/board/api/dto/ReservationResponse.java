package com.condos.board.api.dto;

import com.condos.board.model.Reservation;
import com.condos.board.model.ReservationStatus;

import java.time.Instant;
import java.time.LocalDate;

public record ReservationResponse(
        String id,
        String boardId,
        String orgId,
        String amenityId,
        String unitId,
        String requestedBy,
        LocalDate date,
        Integer peopleCount,
        String note,
        ReservationStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
                r.getId(), r.getBoardId(), r.getOrgId(), r.getAmenityId(), r.getUnitId(),
                r.getRequestedBy(), r.getDate(), r.getPeopleCount(), r.getNote(),
                r.getStatus(), r.getCreatedAt(), r.getUpdatedAt()
        );
    }
}

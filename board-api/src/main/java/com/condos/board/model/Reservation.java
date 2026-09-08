package com.condos.board.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

/** Reservación de un condómino para usar una amenidad en un día específico. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Document("reservations")
@CompoundIndexes({
        @CompoundIndex(name = "idx_amenity_date", def = "{ 'amenityId': 1, 'date': 1 }")
})
public class Reservation {
    @Id
    private String id;

    @Indexed
    private String boardId;

    @Indexed
    private String orgId;

    @Indexed
    private String amenityId;

    @Indexed
    private String unitId;

    private String requestedBy; // userId (condomino) que la creó

    private LocalDate date;
    private Integer peopleCount;
    private String note; // nota opcional del condómino, ej. "cumpleaños"

    private ReservationStatus status;

    private Instant createdAt;
    private Instant updatedAt;

    public static Reservation newReservation(String orgId, String boardId, String amenityId, String unitId,
                                              String requestedBy, LocalDate date, Integer peopleCount, String note) {
        Instant now = Instant.now();
        return Reservation.builder()
                .orgId(orgId)
                .boardId(boardId)
                .amenityId(amenityId)
                .unitId(unitId)
                .requestedBy(requestedBy)
                .date(date)
                .peopleCount(peopleCount)
                .note(note)
                .status(ReservationStatus.CONFIRMED)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}

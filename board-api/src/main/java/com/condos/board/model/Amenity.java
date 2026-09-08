package com.condos.board.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Una amenidad de una colonia (alberca, salón de eventos, cancha, etc.),
 * con las reglas de reservación que el condómino debe ver y que el sistema
 * aplica al crear una reservación (ver ReservationService).
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Document("amenities")
public class Amenity {
    @Id
    private String id;

    @Indexed
    private String boardId;

    @Indexed
    private String orgId;

    private String name;          // "Alberca", "Salón de eventos"
    private String description;   // texto libre, opcional

    // ===== Reglas (todas opcionales = sin límite) =====
    private Integer maxPeoplePerReservation;     // ej. alberca: 20
    private Integer maxReservationsPerUnitPerDay; // ej. 1 reserva por día por casa
    private Integer maxReservationsPerDay;        // capacidad total del día (todas las unidades)
    private Integer advanceBookingDays;           // con cuántos días de anticipación máximo se puede reservar

    private String notes; // reglas/instrucciones libres que ve el condómino al reservar

    private AmenityStatus status;

    private Instant createdAt;
    private Instant updatedAt;

    public static Amenity newAmenity(String orgId, String boardId, String name, String description,
                                      Integer maxPeoplePerReservation, Integer maxReservationsPerUnitPerDay,
                                      Integer maxReservationsPerDay, Integer advanceBookingDays, String notes) {
        Instant now = Instant.now();
        return Amenity.builder()
                .orgId(orgId)
                .boardId(boardId)
                .name(name)
                .description(description)
                .maxPeoplePerReservation(maxPeoplePerReservation)
                .maxReservationsPerUnitPerDay(maxReservationsPerUnitPerDay)
                .maxReservationsPerDay(maxReservationsPerDay)
                .advanceBookingDays(advanceBookingDays)
                .notes(notes)
                .status(AmenityStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}

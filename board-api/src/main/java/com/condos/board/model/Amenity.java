package com.condos.board.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    // ===== Horarios por bloque (opcional): si los 3 están presentes, las
    // reservaciones de esta amenidad deben elegir un horario (RN-RES-02).
    // Si falta alguno, la amenidad se reserva solo por día (comportamiento
    // anterior, sin horario).
    private String openTime;            // "HH:mm", ej. "08:00"
    private String closeTime;           // "HH:mm", ej. "20:00"
    private Integer slotDurationMinutes; // ej. 60 = bloques de una hora

    // Fechas puntuales sin disponibilidad (mantenimiento u otro motivo):
    // nadie puede reservar esta amenidad esos días, sin importar el horario.
    private List<BlockedDate> blockedDates;

    // Bloqueos recurrentes: ej. "todos los martes" (intervalWeeks=1) o
    // "cada 2 semanas los martes" (intervalWeeks=2), a partir de anchorDate
    // (su día de la semana define qué día se repite) y hasta `until` si se
    // indica.
    private List<RecurringBlock> recurringBlocks;

    private String notes; // reglas/instrucciones libres que ve el condómino al reservar

    private AmenityStatus status;

    private Instant createdAt;
    private Instant updatedAt;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BlockedDate {
        private LocalDate date;
        private String reason; // opcional, ej. "Mantenimiento de alberca"
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RecurringBlock {
        private LocalDate anchorDate;  // primera fecha bloqueada; su día de la semana es el que se repite
        private Integer intervalWeeks; // 1 = cada semana, 2 = cada 2 semanas, etc.
        private LocalDate until;       // opcional: última fecha en que aplica (inclusive)
        private String reason;         // opcional, ej. "Mantenimiento semanal"

        public boolean matches(LocalDate date) {
            if (anchorDate == null || date == null) return false;
            if (date.getDayOfWeek() != anchorDate.getDayOfWeek()) return false;
            if (date.isBefore(anchorDate)) return false;
            if (until != null && date.isAfter(until)) return false;
            int iv = (intervalWeeks == null || intervalWeeks < 1) ? 1 : intervalWeeks;
            long weeks = ChronoUnit.DAYS.between(anchorDate, date) / 7;
            return weeks % iv == 0;
        }
    }

    /** Motivo del bloqueo para esta fecha (puntual o recurrente), o null si no está bloqueada. */
    public String blockedReason(LocalDate date) {
        if (blockedDates != null) {
            var one = blockedDates.stream().filter(b -> Objects.equals(b.getDate(), date)).findFirst();
            if (one.isPresent()) return one.get().getReason() != null ? one.get().getReason() : "";
        }
        if (recurringBlocks != null) {
            var rec = recurringBlocks.stream().filter(rb -> rb.matches(date)).findFirst();
            if (rec.isPresent()) return rec.get().getReason() != null ? rec.get().getReason() : "";
        }
        return null;
    }

    public boolean isBlocked(LocalDate date) {
        return blockedReason(date) != null;
    }

    public boolean hasTimeSlots() {
        return openTime != null && !openTime.isBlank()
                && closeTime != null && !closeTime.isBlank()
                && slotDurationMinutes != null && slotDurationMinutes > 0;
    }

    /** Lista de horas de inicio "HH:mm" generadas entre openTime y closeTime, cada slotDurationMinutes. */
    public List<String> generateSlots() {
        List<String> slots = new ArrayList<>();
        if (!hasTimeSlots()) return slots;
        LocalTime t;
        LocalTime end;
        try {
            t = LocalTime.parse(openTime);
            end = LocalTime.parse(closeTime);
        } catch (Exception e) {
            return slots;
        }
        while (!t.plusMinutes(slotDurationMinutes).isAfter(end)) {
            slots.add(t.toString());
            t = t.plusMinutes(slotDurationMinutes);
        }
        return slots;
    }

    public static Amenity newAmenity(String orgId, String boardId, String name, String description,
                                      Integer maxPeoplePerReservation, Integer maxReservationsPerUnitPerDay,
                                      Integer maxReservationsPerDay, Integer advanceBookingDays,
                                      String openTime, String closeTime, Integer slotDurationMinutes,
                                      List<BlockedDate> blockedDates, List<RecurringBlock> recurringBlocks,
                                      String notes) {
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
                .openTime(openTime)
                .closeTime(closeTime)
                .slotDurationMinutes(slotDurationMinutes)
                .blockedDates(blockedDates)
                .recurringBlocks(recurringBlocks)
                .notes(notes)
                .status(AmenityStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}

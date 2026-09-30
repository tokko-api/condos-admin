package com.condos.board.service;

import com.condos.board.api.dto.AmenityAvailabilityResponse;
import com.condos.board.model.Amenity;
import com.condos.board.model.AmenityStatus;
import com.condos.board.model.Reservation;
import com.condos.board.model.ReservationStatus;
import com.condos.board.repository.AmenityRepository;
import com.condos.board.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Crea reservaciones aplicando las reglas configuradas en la Amenity
 * (RN-RES-01): cupo por reservación, tope por unidad al día, tope total del
 * día, y ventana de anticipación. Todo se valida aquí, del lado del
 * servidor — el frontend solo refleja estas reglas para que el condómino
 * las vea antes de intentar reservar.
 */
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository repo;
    private final AmenityRepository amenities;

    public Amenity getAmenityOrThrow(String amenityId) {
        return amenities.findById(amenityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "amenity not found: " + amenityId));
    }

    public Reservation create(String amenityId, String unitId,
                               String requestedBy, LocalDate date, String startTime, Integer peopleCount, String note) {
        Amenity amenity = getAmenityOrThrow(amenityId);

        if (amenity.getStatus() != AmenityStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta amenidad no está disponible para reservar.");
        }

        assertNotBlocked(amenity, date);

        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes reservar una fecha pasada.");
        }
        if (amenity.getAdvanceBookingDays() != null) {
            LocalDate maxDate = today.plusDays(amenity.getAdvanceBookingDays());
            if (date.isAfter(maxDate)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Esta amenidad solo se puede reservar con hasta " + amenity.getAdvanceBookingDays() + " día(s) de anticipación.");
            }
        }

        if (amenity.getMaxPeoplePerReservation() != null
                && peopleCount != null && peopleCount > amenity.getMaxPeoplePerReservation()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Esta amenidad admite máximo " + amenity.getMaxPeoplePerReservation() + " persona(s) por reservación.");
        }

        String resolvedStartTime = resolveStartTime(amenity, startTime);
        String resolvedEndTime = resolvedStartTime == null
                ? null
                : LocalTime.parse(resolvedStartTime).plusMinutes(amenity.getSlotDurationMinutes()).toString();

        if (resolvedStartTime != null) {
            List<Reservation> sameSlot = repo.findByAmenityIdAndDateAndStartTimeAndStatus(
                    amenityId, date, resolvedStartTime, ReservationStatus.CONFIRMED);
            if (!sameSlot.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese horario ya está reservado.");
            }
        }

        List<Reservation> sameUnitSameDay =
                repo.findByAmenityIdAndUnitIdAndDateAndStatus(amenityId, unitId, date, ReservationStatus.CONFIRMED);
        int maxPerUnit = amenity.getMaxReservationsPerUnitPerDay() != null
                ? amenity.getMaxReservationsPerUnitPerDay() : Integer.MAX_VALUE;
        if (sameUnitSameDay.size() >= maxPerUnit) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Tu unidad ya alcanzó el máximo de " + maxPerUnit + " reservación(es) de esta amenidad para ese día.");
        }

        if (amenity.getMaxReservationsPerDay() != null) {
            List<Reservation> sameDay =
                    repo.findByAmenityIdAndDateAndStatus(amenityId, date, ReservationStatus.CONFIRMED);
            if (sameDay.size() >= amenity.getMaxReservationsPerDay()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Ya no hay cupo disponible para esta amenidad ese día.");
            }
        }

        Reservation r = Reservation.newReservation(amenity.getOrgId(), amenity.getBoardId(), amenityId, unitId,
                requestedBy, date, resolvedStartTime, resolvedEndTime, peopleCount, note);
        return repo.save(r);
    }

    /** Lanza 409 si ese día está bloqueado para reservar (fecha puntual o regla recurrente). */
    private void assertNotBlocked(Amenity amenity, LocalDate date) {
        String reason = amenity.blockedReason(date);
        if (reason != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta amenidad no está disponible ese día" + (!reason.isBlank() ? " (" + reason + ")" : "") + ".");
        }
    }

    /**
     * Valida/normaliza el horario elegido contra los slots que ofrece la
     * amenidad. Devuelve null si la amenidad no usa horarios (reserva por
     * día completo).
     */
    private String resolveStartTime(Amenity amenity, String startTime) {
        if (!amenity.hasTimeSlots()) {
            return null; // esta amenidad se reserva por día, no por hora
        }
        if (startTime == null || startTime.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes elegir un horario para esta amenidad.");
        }
        List<String> slots = amenity.generateSlots();
        if (!slots.contains(startTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ese horario no es válido para esta amenidad.");
        }
        return startTime;
    }

    /**
     * Modifica fecha/personas/nota de una reservación existente, re-validando
     * las mismas reglas que al crear (RN-RES-01) pero sin contar la propia
     * reservación entre las que ya ocupan cupo ese día.
     */
    public Reservation update(String id, LocalDate date, String startTime, Integer peopleCount, String note) {
        Reservation r = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "reservation not found: " + id));
        if (r.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta reservación ya no está activa.");
        }
        Amenity amenity = getAmenityOrThrow(r.getAmenityId());

        assertNotBlocked(amenity, date);

        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes reservar una fecha pasada.");
        }
        if (amenity.getAdvanceBookingDays() != null) {
            LocalDate maxDate = today.plusDays(amenity.getAdvanceBookingDays());
            if (date.isAfter(maxDate)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Esta amenidad solo se puede reservar con hasta " + amenity.getAdvanceBookingDays() + " día(s) de anticipación.");
            }
        }

        if (amenity.getMaxPeoplePerReservation() != null
                && peopleCount != null && peopleCount > amenity.getMaxPeoplePerReservation()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Esta amenidad admite máximo " + amenity.getMaxPeoplePerReservation() + " persona(s) por reservación.");
        }

        String resolvedStartTime = resolveStartTime(amenity, startTime);
        String resolvedEndTime = resolvedStartTime == null
                ? null
                : LocalTime.parse(resolvedStartTime).plusMinutes(amenity.getSlotDurationMinutes()).toString();

        if (resolvedStartTime != null) {
            List<Reservation> sameSlot = repo.findByAmenityIdAndDateAndStartTimeAndStatus(
                            r.getAmenityId(), date, resolvedStartTime, ReservationStatus.CONFIRMED)
                    .stream().filter(other -> !other.getId().equals(id)).toList();
            if (!sameSlot.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese horario ya está reservado.");
            }
        }

        List<Reservation> sameUnitSameDay =
                repo.findByAmenityIdAndUnitIdAndDateAndStatus(r.getAmenityId(), r.getUnitId(), date, ReservationStatus.CONFIRMED)
                        .stream().filter(other -> !other.getId().equals(id)).toList();
        int maxPerUnit = amenity.getMaxReservationsPerUnitPerDay() != null
                ? amenity.getMaxReservationsPerUnitPerDay() : Integer.MAX_VALUE;
        if (sameUnitSameDay.size() >= maxPerUnit) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Tu unidad ya alcanzó el máximo de " + maxPerUnit + " reservación(es) de esta amenidad para ese día.");
        }

        if (amenity.getMaxReservationsPerDay() != null) {
            List<Reservation> sameDay =
                    repo.findByAmenityIdAndDateAndStatus(r.getAmenityId(), date, ReservationStatus.CONFIRMED)
                            .stream().filter(other -> !other.getId().equals(id)).toList();
            if (sameDay.size() >= amenity.getMaxReservationsPerDay()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Ya no hay cupo disponible para esta amenidad ese día.");
            }
        }

        r.setDate(date);
        r.setStartTime(resolvedStartTime);
        r.setEndTime(resolvedEndTime);
        r.setPeopleCount(peopleCount);
        r.setNote(note);
        r.setUpdatedAt(Instant.now());
        return repo.save(r);
    }

    public AmenityAvailabilityResponse availability(String amenityId, LocalDate date, String unitId) {
        Amenity amenity = amenities.findById(amenityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "amenity not found: " + amenityId));

        List<Reservation> sameDay =
                repo.findByAmenityIdAndDateAndStatus(amenityId, date, ReservationStatus.CONFIRMED);

        Integer max = amenity.getMaxReservationsPerDay();
        Integer remaining = max == null ? null : Math.max(0, max - sameDay.size());

        boolean unitAlready = unitId != null && sameDay.stream().anyMatch(r -> unitId.equals(r.getUnitId()));

        List<String> allSlots = amenity.generateSlots();
        List<String> takenSlots = sameDay.stream()
                .map(Reservation::getStartTime)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        String blockReason = amenity.blockedReason(date);

        return new AmenityAvailabilityResponse(amenityId, date, max, sameDay.size(), remaining, unitAlready,
                allSlots, takenSlots, blockReason != null, blockReason);
    }

    public Optional<Reservation> get(String id) {
        return repo.findById(id);
    }

    public List<Reservation> listByAmenityAndDate(String amenityId, LocalDate date) {
        return repo.findByAmenityIdAndDateAndStatus(amenityId, date, ReservationStatus.CONFIRMED);
    }

    /** Rango de fechas (from/to inclusive) para una amenidad — reporte por periodo, no solo un día. */
    public List<Reservation> listByAmenity(String amenityId, LocalDate from, LocalDate to) {
        return repo.findByAmenityIdAndDateBetweenAndStatus(amenityId, from, to, ReservationStatus.CONFIRMED);
    }

    public List<Reservation> listByBoard(String boardId, LocalDate from, LocalDate to) {
        return repo.findByBoardIdAndDateBetweenAndStatus(boardId, from, to, ReservationStatus.CONFIRMED);
    }

    public List<Reservation> listByUnit(String unitId) {
        return repo.findByUnitIdOrderByDateDesc(unitId);
    }

    public List<Reservation> listByRequester(String requestedBy) {
        return repo.findByRequestedByOrderByDateDesc(requestedBy);
    }

    public Reservation cancel(String id) {
        Reservation r = repo.findById(id).orElseThrow(NoSuchElementException::new);
        r.setStatus(ReservationStatus.CANCELLED);
        r.setUpdatedAt(Instant.now());
        return repo.save(r);
    }
}

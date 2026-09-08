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
                               String requestedBy, LocalDate date, Integer peopleCount, String note) {
        Amenity amenity = getAmenityOrThrow(amenityId);

        if (amenity.getStatus() != AmenityStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta amenidad no está disponible para reservar.");
        }

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
                requestedBy, date, peopleCount, note);
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

        return new AmenityAvailabilityResponse(amenityId, date, max, sameDay.size(), remaining, unitAlready);
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

package com.condos.board.api;

import com.condos.board.api.dto.AmenityAvailabilityResponse;
import com.condos.board.api.dto.CreateReservationRequest;
import com.condos.board.api.dto.ReservationResponse;
import com.condos.board.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/condos/api/board")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservations;

    // ======== CREATE (condómino) ========
    @PostMapping("/amenities/{amenityId}/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("""
        @jwtAuth.isResidentOfAmenityBoard(authentication, #amenityId)
        and @jwtAuth.isResidentOfUnit(authentication, #req.unitId())
    """)
    public ReservationResponse create(@PathVariable String amenityId,
                                       @Valid @RequestBody CreateReservationRequest req,
                                       Authentication authentication) {
        var r = reservations.create(amenityId, req.unitId(),
                authentication.getName(), req.date(), req.peopleCount(), req.note());
        return ReservationResponse.from(r);
    }

    // ======== DISPONIBILIDAD (staff o residente de la colonia) ========
    @GetMapping("/amenities/{amenityId}/availability")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToAmenity(authentication, #amenityId, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'}) or
        @jwtAuth.isResidentOfAmenityBoard(authentication, #amenityId)
    """)
    public AmenityAvailabilityResponse availability(
            @PathVariable String amenityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String unitId) {
        return reservations.availability(amenityId, date, unitId);
    }

    // ======== LIST por amenidad: un día, o un rango (reporte) — staff ========
    @GetMapping("/amenities/{amenityId}/reservations")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToAmenity(authentication, #amenityId, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})
    """)
    public List<ReservationResponse> listByAmenity(
            @PathVariable String amenityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate effFrom = from != null ? from : date;
        LocalDate effTo = to != null ? to : date;
        if (effFrom == null || effTo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes indicar date, o from y to");
        }
        return reservations.listByAmenity(amenityId, effFrom, effTo).stream().map(ReservationResponse::from).toList();
    }

    // ======== LIST por colonia y rango de fechas (staff/operador) ========
    @GetMapping("/boards/{boardId}/reservations")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToBoard(authentication, #boardId, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})
    """)
    public List<ReservationResponse> listByBoard(
            @PathVariable String boardId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reservations.listByBoard(boardId, from, to).stream().map(ReservationResponse::from).toList();
    }

    // ======== MIS RESERVAS (condómino) ========
    @GetMapping("/reservations/mine")
    public List<ReservationResponse> mine(Authentication authentication) {
        return reservations.listByRequester(authentication.getName()).stream().map(ReservationResponse::from).toList();
    }

    // ======== CANCELAR (dueño o staff) ========
    @PatchMapping("/reservations/{id}/cancel")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.isRequesterOfReservation(authentication, #id) or
        @jwtAuth.hasAccessToReservation(authentication, #id, {'ADMINISTRADOR','SUPERVISOR'})
    """)
    public ReservationResponse cancel(@PathVariable String id) {
        var r = reservations.cancel(id);
        return ReservationResponse.from(r);
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public java.util.Map<String, Object> onNotFound(RuntimeException ex) {
        return java.util.Map.of("error", "not_found", "message", ex.getMessage());
    }
}

package com.condos.board.api;

import com.condos.board.api.dto.AmenityResponse;
import com.condos.board.api.dto.AmenityStatusReq;
import com.condos.board.api.dto.CreateAmenityRequest;
import com.condos.board.api.dto.UpdateAmenityRequest;
import com.condos.board.service.AmenityService;
import com.condos.board.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/condos/api/board")
@RequiredArgsConstructor
public class AmenityController {

    private final AmenityService amenities;
    private final BoardService boards;

    // ======== CREATE ========
    @PostMapping("/boards/{boardId}/amenities")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToBoard(authentication, #boardId, {'ADMINISTRADOR','SUPERVISOR'})
    """)
    public AmenityResponse create(@PathVariable String boardId,
                                   @Valid @RequestBody CreateAmenityRequest req) {
        var board = boards.get(boardId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var a = amenities.create(board.orgId, boardId, req.name(), req.description(),
                req.maxPeoplePerReservation(), req.maxReservationsPerUnitPerDay(),
                req.maxReservationsPerDay(), req.advanceBookingDays(), req.notes());
        return AmenityResponse.from(a);
    }

    // ======== LIST by board (colonia): staff o residente ========
    @GetMapping("/boards/{boardId}/amenities")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToBoard(authentication, #boardId, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'}) or
        @jwtAuth.isResidentOfBoard(authentication, #boardId)
    """)
    public List<AmenityResponse> list(@PathVariable String boardId,
                                       @RequestParam(defaultValue = "false") boolean includeInactive) {
        return amenities.listByBoard(boardId, includeInactive).stream().map(AmenityResponse::from).toList();
    }

    // ======== READ ONE ========
    @GetMapping("/amenities/{id}")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToAmenity(authentication, #id, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'}) or
        @jwtAuth.isResidentOfAmenityBoard(authentication, #id)
    """)
    public AmenityResponse get(@PathVariable String id) {
        var a = amenities.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return AmenityResponse.from(a);
    }

    // ======== UPDATE ========
    @PutMapping("/amenities/{id}")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToAmenity(authentication, #id, {'ADMINISTRADOR','SUPERVISOR'})
    """)
    public AmenityResponse update(@PathVariable String id, @Valid @RequestBody UpdateAmenityRequest req) {
        var a = amenities.update(id, req.name(), req.description(), req.maxPeoplePerReservation(),
                req.maxReservationsPerUnitPerDay(), req.maxReservationsPerDay(), req.advanceBookingDays(), req.notes());
        return AmenityResponse.from(a);
    }

    // ======== CHANGE STATUS ========
    @PatchMapping("/amenities/{id}/status")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToAmenity(authentication, #id, {'ADMINISTRADOR','SUPERVISOR'})
    """)
    public AmenityResponse changeStatus(@PathVariable String id, @RequestBody AmenityStatusReq req) {
        var a = amenities.changeStatus(id, req.status());
        return AmenityResponse.from(a);
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public java.util.Map<String, Object> onNotFound(RuntimeException ex) {
        return java.util.Map.of("error", "not_found", "message", ex.getMessage());
    }
}

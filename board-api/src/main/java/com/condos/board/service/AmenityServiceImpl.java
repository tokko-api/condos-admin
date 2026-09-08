package com.condos.board.service;

import com.condos.board.model.Amenity;
import com.condos.board.model.AmenityStatus;
import com.condos.board.repository.AmenityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AmenityServiceImpl implements AmenityService {

    private final AmenityRepository repo;

    @Override
    public Amenity create(String orgId, String boardId, String name, String description,
                           Integer maxPeoplePerReservation, Integer maxReservationsPerUnitPerDay,
                           Integer maxReservationsPerDay, Integer advanceBookingDays, String notes) {
        Amenity a = Amenity.newAmenity(orgId, boardId, name, description, maxPeoplePerReservation,
                maxReservationsPerUnitPerDay, maxReservationsPerDay, advanceBookingDays, notes);
        return repo.save(a);
    }

    @Override
    public Optional<Amenity> get(String id) {
        return repo.findById(id);
    }

    @Override
    public List<Amenity> listByBoard(String boardId, boolean includeInactive) {
        return includeInactive
                ? repo.findByBoardId(boardId)
                : repo.findByBoardIdAndStatus(boardId, AmenityStatus.ACTIVE);
    }

    @Override
    public Amenity update(String id, String name, String description,
                           Integer maxPeoplePerReservation, Integer maxReservationsPerUnitPerDay,
                           Integer maxReservationsPerDay, Integer advanceBookingDays, String notes) {
        Amenity a = repo.findById(id).orElseThrow(() -> notFound(id));

        if (StringUtils.hasText(name)) a.setName(name);
        a.setDescription(description);
        // Reglas: reemplazo completo (no "merge parcial") — el formulario de
        // administración siempre manda todas las reglas, así que dejar un
        // campo en blanco significa "sin límite", no "no tocar".
        a.setMaxPeoplePerReservation(maxPeoplePerReservation);
        a.setMaxReservationsPerUnitPerDay(maxReservationsPerUnitPerDay);
        a.setMaxReservationsPerDay(maxReservationsPerDay);
        a.setAdvanceBookingDays(advanceBookingDays);
        a.setNotes(notes);

        a.setUpdatedAt(Instant.now());
        return repo.save(a);
    }

    @Override
    public Amenity changeStatus(String id, AmenityStatus status) {
        Amenity a = repo.findById(id).orElseThrow(() -> notFound(id));
        a.setStatus(status);
        a.setUpdatedAt(Instant.now());
        return repo.save(a);
    }

    private ResponseStatusException notFound(String id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "amenity not found: " + id);
    }
}

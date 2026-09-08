package com.condos.board.service;

import com.condos.board.model.Amenity;
import com.condos.board.model.AmenityStatus;

import java.util.List;
import java.util.Optional;

public interface AmenityService {

    Amenity create(String orgId, String boardId, String name, String description,
                    Integer maxPeoplePerReservation, Integer maxReservationsPerUnitPerDay,
                    Integer maxReservationsPerDay, Integer advanceBookingDays, String notes);

    Optional<Amenity> get(String id);

    List<Amenity> listByBoard(String boardId, boolean includeInactive);

    Amenity update(String id, String name, String description,
                    Integer maxPeoplePerReservation, Integer maxReservationsPerUnitPerDay,
                    Integer maxReservationsPerDay, Integer advanceBookingDays, String notes);

    Amenity changeStatus(String id, AmenityStatus status);
}

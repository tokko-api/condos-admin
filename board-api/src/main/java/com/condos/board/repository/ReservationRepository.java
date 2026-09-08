package com.condos.board.repository;

import com.condos.board.model.Reservation;
import com.condos.board.model.ReservationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface ReservationRepository extends MongoRepository<Reservation, String> {

    List<Reservation> findByAmenityIdAndDateAndStatus(String amenityId, LocalDate date, ReservationStatus status);

    List<Reservation> findByAmenityIdAndDateBetweenAndStatus(
            String amenityId, LocalDate from, LocalDate to, ReservationStatus status);

    List<Reservation> findByAmenityIdAndUnitIdAndDateAndStatus(
            String amenityId, String unitId, LocalDate date, ReservationStatus status);

    List<Reservation> findByBoardIdAndDateAndStatus(String boardId, LocalDate date, ReservationStatus status);

    List<Reservation> findByBoardIdAndDateBetweenAndStatus(
            String boardId, LocalDate from, LocalDate to, ReservationStatus status);

    List<Reservation> findByUnitIdOrderByDateDesc(String unitId);

    List<Reservation> findByRequestedByOrderByDateDesc(String requestedBy);
}

package com.condos.board.api.dto;

import java.time.LocalDate;

/** Disponibilidad de una amenidad para un día específico, pensado para mostrarle al condómino antes de reservar. */
public record AmenityAvailabilityResponse(
        String amenityId,
        LocalDate date,
        Integer maxReservationsPerDay,   // null = sin límite
        int confirmedCount,              // reservaciones confirmadas ese día (todas las unidades)
        Integer remaining,                // null = sin límite; si no, maxReservationsPerDay - confirmedCount (mínimo 0)
        boolean unitAlreadyReservedToday  // true si la unidad que consulta ya tiene una reservación confirmada ese día
) {}

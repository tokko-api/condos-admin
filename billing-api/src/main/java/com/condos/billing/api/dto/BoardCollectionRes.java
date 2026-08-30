package com.condos.billing.api.dto;

import java.math.BigDecimal;

/**
 * Cobranza del periodo agrupada por condominio (boardId).
 * El nombre del condominio se resuelve en el frontend (que ya tiene la
 * lista de boards vía board-api) para no acoplar billing-api a board-api
 * más de lo necesario.
 */
public record BoardCollectionRes(
        String boardId,
        BigDecimal billed,
        BigDecimal collected,
        double percentage
) {}

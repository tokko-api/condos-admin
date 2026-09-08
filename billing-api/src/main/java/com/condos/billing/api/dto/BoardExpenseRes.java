package com.condos.billing.api.dto;

import java.math.BigDecimal;

/** Egresos del periodo agrupados por condominio (boardId), para el estado financiero. */
public record BoardExpenseRes(
        String boardId,
        BigDecimal totalExpenses
) {}

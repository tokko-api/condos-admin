package com.condos.billing.api.dto;

import com.condos.billing.model.ExpenseCategory;

import java.math.BigDecimal;

/** Desglose de egresos por categoría, de una colonia en un periodo. */
public record ExpenseCategoryBreakdownRes(
        ExpenseCategory category,
        BigDecimal total
) {}

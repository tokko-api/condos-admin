package com.condos.billing.api.dto;

import com.condos.billing.model.ExpenseCategory;
import com.condos.billing.model.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateExpenseRequest(
        @NotBlank String concept,
        @NotNull ExpenseCategory category,
        String providerName,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull PaymentMethod method,
        @NotNull LocalDate expenseDate,
        String receiptFileId,
        String receiptFileName,
        String notes
) {}

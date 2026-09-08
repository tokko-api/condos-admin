package com.condos.billing.api.dto;

import com.condos.billing.model.Expense;
import com.condos.billing.model.ExpenseCategory;
import com.condos.billing.model.ExpenseStatus;
import com.condos.billing.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExpenseResponse(
        String id,
        String boardId,
        String orgId,
        String concept,
        ExpenseCategory category,
        String providerName,
        BigDecimal amount,
        PaymentMethod method,
        LocalDate expenseDate,
        String period,
        String receiptFileId,
        String receiptFileName,
        String notes,
        String registeredBy,
        ExpenseStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ExpenseResponse from(Expense e) {
        return new ExpenseResponse(
                e.getId(), e.getBoardId(), e.getOrgId(), e.getConcept(), e.getCategory(),
                e.getProviderName(), e.getAmount(), e.getMethod(), e.getExpenseDate(), e.getPeriod(),
                e.getReceiptFileId(), e.getReceiptFileName(), e.getNotes(), e.getRegisteredBy(),
                e.getStatus(), e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}

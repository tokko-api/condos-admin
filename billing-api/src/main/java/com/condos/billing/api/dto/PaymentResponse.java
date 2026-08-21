package com.condos.billing.api.dto;

import com.condos.billing.model.Payment;
import com.condos.billing.model.PaymentMethod;
import com.condos.billing.model.ReconciliationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PaymentResponse(
        String id,
        String boardId,
        String orgId,
        String unitId,
        List<String> chargeIds,
        BigDecimal amount,
        PaymentMethod method,
        String receiptFileId,
        ReconciliationStatus reconciliationStatus,
        String reconciledBy,
        Instant reconciledAt,
        Instant reportedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
                p.getId(), p.getBoardId(), p.getOrgId(), p.getUnitId(), p.getChargeIds(), p.getAmount(),
                p.getMethod(), p.getReceiptFileId(), p.getReconciliationStatus(), p.getReconciledBy(),
                p.getReconciledAt(), p.getReportedAt(), p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}

package com.condos.billing.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Un pago registrado por una unidad, que puede cubrir uno o más cargos. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Document("payments")
public class Payment {
    @Id
    private String id;

    @Indexed
    private String boardId;

    @Indexed
    private String orgId;

    @Indexed
    private String unitId;

    private List<String> chargeIds;
    private BigDecimal amount;
    private PaymentMethod method;
    private String receiptFileId; // referencia a archivo subido vía board-api/files, opcional

    private ReconciliationStatus reconciliationStatus;
    private String reconciledBy;
    private Instant reconciledAt;

    private Instant reportedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static Payment newPayment(String orgId, String boardId, String unitId, List<String> chargeIds,
                                      BigDecimal amount, PaymentMethod method, String receiptFileId) {
        Instant now = Instant.now();
        return Payment.builder()
                .orgId(orgId)
                .boardId(boardId)
                .unitId(unitId)
                .chargeIds(chargeIds)
                .amount(amount)
                .method(method)
                .receiptFileId(receiptFileId)
                .reconciliationStatus(ReconciliationStatus.PENDING)
                .reportedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}

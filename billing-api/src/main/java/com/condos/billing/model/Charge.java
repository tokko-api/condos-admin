package com.condos.billing.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/** Un cargo (cuota regular o extraordinaria) generado para una unidad específica. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Document("charges")
@CompoundIndexes({
        // evita generar dos veces el mismo cargo regular para la misma unidad y periodo
        @CompoundIndex(name = "uq_unit_schedule_period", def = "{ 'unitId': 1, 'feeScheduleId': 1, 'period': 1 }",
                unique = true, sparse = true)
})
public class Charge {
    @Id
    private String id;

    @Indexed
    private String boardId;

    @Indexed
    private String orgId;

    @Indexed
    private String unitId;

    private String feeScheduleId; // null si es extraordinaria
    private ChargeType type;
    private String concept;
    private BigDecimal amount;
    private String period;        // "2026-08" para regulares; null en extraordinarias sin periodo fijo
    private Instant dueDate;
    private ChargeStatus status;

    /**
     * Cuánto se ha aplicado realmente a este cargo a través de pagos
     * conciliados (ver PaymentServiceImpl.reconcile). Nunca excede
     * `amount`: lo que sobra de un pago se registra como crédito de la
     * unidad (UnitCredit) en vez de inflar este campo.
     */
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    private AssemblyRef assemblyRef; // solo para EXTRAORDINARY

    private Instant createdAt;
    private Instant updatedAt;

    public static Charge newCharge(String orgId, String boardId, String unitId, String feeScheduleId,
                                    ChargeType type, String concept, BigDecimal amount, String period,
                                    Instant dueDate, AssemblyRef assemblyRef) {
        Instant now = Instant.now();
        return Charge.builder()
                .orgId(orgId)
                .boardId(boardId)
                .unitId(unitId)
                .feeScheduleId(feeScheduleId)
                .type(type)
                .concept(concept)
                .amount(amount)
                .period(period)
                .dueDate(dueDate)
                .status(ChargeStatus.PENDING)
                .paidAmount(BigDecimal.ZERO)
                .assemblyRef(assemblyRef)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}

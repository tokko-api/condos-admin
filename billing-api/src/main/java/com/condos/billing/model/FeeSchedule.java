package com.condos.billing.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Configuración de una cuota recurrente para una colonia (Board). Se define
 * una vez y, al generar cargos (ChargeService), se crea un Charge por cada
 * Unit activa de ese boardId con este mismo monto.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Document("fee_schedules")
public class FeeSchedule {
    @Id
    private String id;

    @Indexed
    private String boardId;

    @Indexed
    private String orgId;

    private String name;            // "Cuota de mantenimiento mensual"
    private BigDecimal amount;      // monto por unidad
    private String currency;        // "MXN"
    private FeeFrequency frequency;
    private int dueDayOfPeriod;     // día del mes en que vence, ej. 5

    private boolean active;

    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;

    public static FeeSchedule newSchedule(String orgId, String boardId, String name, BigDecimal amount,
                                           String currency, FeeFrequency frequency, int dueDayOfPeriod,
                                           String createdBy) {
        Instant now = Instant.now();
        return FeeSchedule.builder()
                .orgId(orgId)
                .boardId(boardId)
                .name(name)
                .amount(amount)
                .currency(currency)
                .frequency(frequency)
                .dueDayOfPeriod(dueDayOfPeriod)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .createdBy(createdBy)
                .build();
    }
}

package com.condos.billing.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Saldo a favor de una unidad (RN-PAG-05 ampliada): cuando un pago
 * conciliado excede lo que se debía en los cargos que cubre, el excedente
 * se guarda aquí en vez de perderse en un balance negativo sin explicar.
 * Un documento por unidad (el id del documento ES el unitId).
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Document("unit_credits")
public class UnitCredit {
    @Id
    private String unitId;

    private String orgId;
    private String boardId;
    private BigDecimal balance;

    private Instant updatedAt;
}

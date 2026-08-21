package com.condos.board.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Una casa/departamento dentro de una colonia (Board).
 * La facturación de cuotas (billing-api) se hace por unitId, no por boardId.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Document("units")
@CompoundIndexes({
        @CompoundIndex(name = "uq_board_identifier", def = "{ 'boardId': 1, 'identifier': 1 }", unique = true)
})
public class Unit {
    @Id
    private String id;

    @Indexed
    private String boardId;    // colonia/condominio a la que pertenece

    @Indexed
    private String orgId;

    private String identifier;      // "Casa 12", "Depto 4B"
    private String ownerName;       // condómino/dueño, texto libre por ahora
    private String residentUserId;  // opcional: si el residente tiene cuenta en el sistema (user-api)
    private BigDecimal coefficient; // % de indiviso, opcional (prorrateo futuro de cuotas/voto)

    private UnitStatus status;

    private Instant createdAt;
    private Instant updatedAt;

    public static Unit newUnit(String orgId, String boardId, String identifier, String ownerName,
                                String residentUserId, BigDecimal coefficient) {
        Instant now = Instant.now();
        return Unit.builder()
                .orgId(orgId)
                .boardId(boardId)
                .identifier(identifier)
                .ownerName(ownerName)
                .residentUserId(residentUserId)
                .coefficient(coefficient)
                .status(UnitStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}

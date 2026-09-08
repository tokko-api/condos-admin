package com.condos.billing.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Un egreso de una colonia: pago a un proveedor, servicio, nómina, etc.
 * Es el contrapeso de Charge/Payment (que registran lo que ENTRA) — esto
 * registra lo que SALE, para poder armar el estado financiero completo
 * (cobranza vs. gasto) por colonia y por periodo.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Document("expenses")
public class Expense {
    @Id
    private String id;

    @Indexed
    private String boardId;

    @Indexed
    private String orgId;

    private String concept;          // "Pago de nómina jardinero", "Recibo CFE agosto"
    private ExpenseCategory category;
    private String providerName;     // opcional: a quién se le pagó
    private BigDecimal amount;
    private PaymentMethod method;
    private LocalDate expenseDate;   // cuándo se pagó/incurrió
    private String period;           // "yyyy-MM", derivado de expenseDate — para agregados mensuales

    private String receiptFileId;    // factura/comprobante, opcional (mismo bucket de /files)
    private String receiptFileName;

    private String notes;
    private String registeredBy;     // userId (staff) que lo registró

    private ExpenseStatus status;

    private Instant createdAt;
    private Instant updatedAt;

    public static Expense newExpense(String orgId, String boardId, String concept, ExpenseCategory category,
                                      String providerName, BigDecimal amount, PaymentMethod method,
                                      LocalDate expenseDate, String receiptFileId, String receiptFileName,
                                      String notes, String registeredBy) {
        Instant now = Instant.now();
        return Expense.builder()
                .orgId(orgId)
                .boardId(boardId)
                .concept(concept)
                .category(category)
                .providerName(providerName)
                .amount(amount)
                .method(method)
                .expenseDate(expenseDate)
                .period(expenseDate != null ? expenseDate.toString().substring(0, 7) : null)
                .receiptFileId(receiptFileId)
                .receiptFileName(receiptFileName)
                .notes(notes)
                .registeredBy(registeredBy)
                .status(ExpenseStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}

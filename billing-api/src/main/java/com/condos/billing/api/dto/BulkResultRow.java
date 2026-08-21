package com.condos.billing.api.dto;

public record BulkResultRow(
        String reference,   // paymentId o el identificador de unidad, según la operación
        String status,      // "OK" | "ERROR"
        String message,
        String paymentId,   // solo en bulk-import: el pago creado (null si error)
        Integer coveredChargesCount
) {
    public static BulkResultRow ok(String reference, String message, String paymentId, Integer coveredChargesCount) {
        return new BulkResultRow(reference, "OK", message, paymentId, coveredChargesCount);
    }

    public static BulkResultRow error(String reference, String message) {
        return new BulkResultRow(reference, "ERROR", message, null, null);
    }
}

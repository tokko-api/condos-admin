package com.condos.billing.service;

import com.condos.billing.api.dto.BulkImportPaymentRow;
import com.condos.billing.api.dto.BulkResultRow;
import com.condos.billing.model.Charge;
import com.condos.billing.model.ChargeStatus;
import com.condos.billing.model.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Importa pagos en lote desde un archivo (Excel/CSV) que el frontend ya
 * parseó a filas { unitIdentifier, amount, method }.
 *
 * Para cada fila:
 *  1. Resuelve unitIdentifier -> unitId consultando board-api (BoardApiClient).
 *  2. Toma los cargos PENDING de esa unidad, ordenados por vencimiento, y
 *     selecciona los necesarios hasta cubrir el monto informado (simplificación
 *     Fase 1: no reparte montos parciales entre cargos, ver PaymentServiceImpl).
 *  3. Crea el Payment con esos chargeIds.
 * Nunca lanza excepción por fila: cada una reporta su propio resultado, para
 * que una fila con error no tumbe la importación completa.
 */
@Service
@RequiredArgsConstructor
public class PaymentBulkImportService {

    private final BoardApiClient boardApiClient;
    private final ChargeService chargeService;
    private final PaymentService paymentService;

    public List<BulkResultRow> importPayments(String orgId, String boardId,
                                               List<BulkImportPaymentRow> rows, String bearerToken) {
        var units = boardApiClient.listActiveUnitIds(boardId, bearerToken);
        List<BulkResultRow> results = new ArrayList<>();

        for (BulkImportPaymentRow row : rows) {
            String identifier = row.unitIdentifier() == null ? "" : row.unitIdentifier().trim();
            try {
                var unit = units.stream()
                        .filter(u -> u.identifier().trim().equalsIgnoreCase(identifier))
                        .findFirst();

                if (unit.isEmpty()) {
                    results.add(BulkResultRow.error(identifier, "Unidad no encontrada en esta colonia: \"" + identifier + "\""));
                    continue;
                }

                List<Charge> pending = chargeService
                        .listByUnit(unit.get().id(), ChargeStatus.PENDING, 0, 200, "dueDate", Sort.Direction.ASC)
                        .getContent();

                if (pending.isEmpty()) {
                    results.add(BulkResultRow.error(identifier, "La unidad no tiene cargos pendientes"));
                    continue;
                }

                List<String> chosen = new ArrayList<>();
                BigDecimal running = BigDecimal.ZERO;
                for (Charge c : pending) {
                    if (running.compareTo(row.amount()) >= 0) break;
                    chosen.add(c.getId());
                    running = running.add(c.getAmount());
                }

                PaymentMethod method = row.method() != null ? row.method() : PaymentMethod.TRANSFER;
                var payment = paymentService.create(orgId, boardId, unit.get().id(), chosen, row.amount(), method, null);

                results.add(BulkResultRow.ok(
                        identifier,
                        "Pago registrado, cubre " + chosen.size() + " cargo(s) (pendiente de conciliar)",
                        payment.getId(),
                        chosen.size()
                ));
            } catch (Exception e) {
                results.add(BulkResultRow.error(identifier, e.getMessage()));
            }
        }
        return results;
    }
}

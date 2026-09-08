package com.condos.billing.service;

import com.condos.billing.api.dto.BulkResultRow;
import com.condos.billing.model.*;
import com.condos.billing.repository.ChargeRepository;
import com.condos.billing.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository payments;
    private final ChargeRepository charges;
    private final CreditService credits;

    @Override
    public Payment create(String orgId, String boardId, String unitId, List<String> chargeIds,
                           BigDecimal amount, PaymentMethod method, String receiptFileId) {
        // valida que los cargos existan y pertenezcan a la misma unidad
        for (String chargeId : chargeIds) {
            Charge c = charges.findById(chargeId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "charge not found: " + chargeId));
            if (!unitId.equals(c.getUnitId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El cargo " + chargeId + " no pertenece a la unidad " + unitId);
            }
        }
        Payment p = Payment.newPayment(orgId, boardId, unitId, chargeIds, amount, method, receiptFileId);
        return payments.save(p);
    }

    @Override
    public Optional<Payment> get(String id) {
        return payments.findById(id);
    }

    @Override
    public Payment reconcile(String id, boolean approve, String reconciledBy) {
        Payment p = payments.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "payment not found: " + id));

        p.setReconciliationStatus(approve ? ReconciliationStatus.RECONCILED : ReconciliationStatus.REJECTED);
        p.setReconciledBy(reconciledBy);
        p.setReconciledAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        payments.save(p);

        // Un pago conciliado se reparte entre los cargos que cubre (los más
        // próximos a vencer primero), sin exceder nunca lo que cada cargo
        // debe. Cada cargo queda PARTIALLY_PAID o PAID según cuánto se le
        // haya aplicado en total (puede recibir aportes de varios pagos). Lo
        // que sobra después de cubrir todos los cargos ligados al pago se
        // guarda como saldo a favor de la unidad (RN-PAG-05 ampliada) en vez
        // de perderse en un balance negativo sin explicar.
        if (approve && p.getChargeIds() != null && !p.getChargeIds().isEmpty()) {
            BigDecimal remaining = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;

            List<Charge> linkedCharges = p.getChargeIds().stream()
                    .map(charges::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .sorted(Comparator.comparing(Charge::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();

            for (Charge c : linkedCharges) {
                if (remaining.signum() <= 0) break;

                BigDecimal chargeAmount = c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO;
                BigDecimal already = c.getPaidAmount() != null ? c.getPaidAmount() : BigDecimal.ZERO;
                BigDecimal due = chargeAmount.subtract(already);
                if (due.signum() <= 0) continue; // ya estaba cubierto por pagos anteriores

                BigDecimal applied = remaining.min(due);
                BigDecimal newPaid = already.add(applied);

                c.setPaidAmount(newPaid);
                c.setStatus(newPaid.compareTo(chargeAmount) >= 0 ? ChargeStatus.PAID : ChargeStatus.PARTIALLY_PAID);
                c.setUpdatedAt(Instant.now());
                charges.save(c);

                remaining = remaining.subtract(applied);
            }

            if (remaining.signum() > 0) {
                credits.addCredit(p.getOrgId(), p.getBoardId(), p.getUnitId(), remaining);
            }
        }

        return p;
    }

    @Override
    public List<BulkResultRow> bulkReconcile(String orgId, List<String> paymentIds, boolean approve, String reconciledBy) {
        List<BulkResultRow> results = new ArrayList<>();
        for (String id : paymentIds) {
            try {
                Payment p = payments.findById(id).orElse(null);
                if (p == null) {
                    results.add(BulkResultRow.error(id, "Pago no encontrado"));
                    continue;
                }
                if (!Objects.equals(orgId, p.getOrgId())) {
                    results.add(BulkResultRow.error(id, "El pago no pertenece a esta organización"));
                    continue;
                }
                reconcile(id, approve, reconciledBy);
                results.add(BulkResultRow.ok(id, approve ? "Conciliado" : "Rechazado", id, null));
            } catch (Exception e) {
                results.add(BulkResultRow.error(id, e.getMessage()));
            }
        }
        return results;
    }

    @Override
    public Page<Payment> listByUnit(String unitId, ReconciliationStatus status, int page, int size, String sortBy, Sort.Direction dir) {
        var pageable = PageRequest.of(page, size, Sort.by(dir, defaultSort(sortBy)));
        return status != null
                ? payments.findByUnitIdAndReconciliationStatus(unitId, status, pageable)
                : payments.findByUnitId(unitId, pageable);
    }

    @Override
    public Page<Payment> listByBoard(String boardId, ReconciliationStatus status, int page, int size, String sortBy, Sort.Direction dir) {
        var pageable = PageRequest.of(page, size, Sort.by(dir, defaultSort(sortBy)));
        return status != null
                ? payments.findByBoardIdAndReconciliationStatus(boardId, status, pageable)
                : payments.findByBoardId(boardId, pageable);
    }

    private String defaultSort(String sortBy) {
        return (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
    }
}

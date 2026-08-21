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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository payments;
    private final ChargeRepository charges;

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

        // NOTA (simplificación Fase 1): un pago conciliado marca como PAID
        // todos los cargos que cubre, sin prorratear montos parciales entre
        // varios cargos. Si en el futuro se necesita pago parcial exacto por
        // cargo, hay que sumar pagos reconciliados por chargeId y comparar
        // contra Charge.amount para decidir PARTIALLY_PAID vs PAID.
        if (approve && p.getChargeIds() != null) {
            for (String chargeId : p.getChargeIds()) {
                charges.findById(chargeId).ifPresent(c -> {
                    c.setStatus(ChargeStatus.PAID);
                    c.setUpdatedAt(Instant.now());
                    charges.save(c);
                });
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

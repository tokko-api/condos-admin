package com.condos.billing.service;

import com.condos.billing.api.dto.BulkResultRow;
import com.condos.billing.model.Payment;
import com.condos.billing.model.PaymentMethod;
import com.condos.billing.model.ReconciliationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentService {

    Payment create(String orgId, String boardId, String unitId, List<String> chargeIds,
                    BigDecimal amount, PaymentMethod method, String receiptFileId);

    Optional<Payment> get(String id);

    Payment reconcile(String id, boolean approve, String reconciledBy);

    /** Concilia/rechaza varios pagos en lote, validando que cada uno pertenezca a orgId. Nunca lanza: reporta error por fila. */
    List<BulkResultRow> bulkReconcile(String orgId, List<String> paymentIds, boolean approve, String reconciledBy);

    Page<Payment> listByUnit(String unitId, ReconciliationStatus status, int page, int size, String sortBy, Sort.Direction dir);

    Page<Payment> listByBoard(String boardId, ReconciliationStatus status, int page, int size, String sortBy, Sort.Direction dir);
}

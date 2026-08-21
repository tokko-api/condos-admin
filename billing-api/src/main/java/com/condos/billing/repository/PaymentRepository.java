package com.condos.billing.repository;

import com.condos.billing.model.Payment;
import com.condos.billing.model.ReconciliationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    Page<Payment> findByUnitId(String unitId, Pageable pageable);
    Page<Payment> findByUnitIdAndReconciliationStatus(String unitId, ReconciliationStatus status, Pageable pageable);

    Page<Payment> findByBoardId(String boardId, Pageable pageable);
    Page<Payment> findByBoardIdAndReconciliationStatus(String boardId, ReconciliationStatus status, Pageable pageable);
}

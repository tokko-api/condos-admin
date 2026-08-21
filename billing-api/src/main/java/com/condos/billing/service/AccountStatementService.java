package com.condos.billing.service;

import com.condos.billing.api.dto.AccountStatementResponse;
import com.condos.billing.api.dto.ChargeResponse;
import com.condos.billing.api.dto.PaymentResponse;
import com.condos.billing.model.Charge;
import com.condos.billing.model.Payment;
import com.condos.billing.model.ReconciliationStatus;
import com.condos.billing.repository.ChargeRepository;
import com.condos.billing.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountStatementService {

    private final ChargeRepository chargeRepo;
    private final PaymentRepository paymentRepo;

    /** Estado de cuenta on-the-fly de una unidad: agrega charges + payments (RN-PAG-05). */
    public AccountStatementResponse forUnit(String unitId) {
        List<Charge> charges = chargeRepo
                .findByUnitId(unitId, PageRequest.of(0, 500, Sort.by(Sort.Direction.ASC, "dueDate")))
                .getContent();
        List<Payment> payments = paymentRepo
                .findByUnitId(unitId, PageRequest.of(0, 500, Sort.by(Sort.Direction.ASC, "createdAt")))
                .getContent();

        BigDecimal totalCharged = charges.stream()
                .map(Charge::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = payments.stream()
                .filter(p -> p.getReconciliationStatus() == ReconciliationStatus.RECONCILED)
                .map(Payment::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String boardId = !charges.isEmpty() ? charges.get(0).getBoardId()
                : (!payments.isEmpty() ? payments.get(0).getBoardId() : null);

        return new AccountStatementResponse(
                unitId,
                boardId,
                totalCharged,
                totalPaid,
                totalCharged.subtract(totalPaid),
                charges.stream().map(ChargeResponse::from).toList(),
                payments.stream().map(PaymentResponse::from).toList()
        );
    }
}

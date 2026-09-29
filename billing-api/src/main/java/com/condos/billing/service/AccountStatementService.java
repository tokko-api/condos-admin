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
    private final CreditService credits;

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
                credits.getBalance(unitId),
                charges.stream().map(ChargeResponse::from).toList(),
                payments.stream().map(PaymentResponse::from).toList()
        );
    }

    /** Exporta un estado de cuenta a CSV (RN-PAG-05): resumen + cargos + pagos. */
    public String toCsv(AccountStatementResponse st) {
        StringBuilder sb = new StringBuilder();
        sb.append("Estado de cuenta,Unidad ").append(csv(st.unitId())).append('\n');
        sb.append("Total cargado,").append(st.totalCharged()).append('\n');
        sb.append("Total pagado,").append(st.totalPaid()).append('\n');
        sb.append("Saldo,").append(st.balance()).append('\n');
        sb.append("Saldo a favor,").append(st.creditBalance() != null ? st.creditBalance() : BigDecimal.ZERO).append('\n');
        sb.append('\n');

        sb.append("Cargos\n");
        sb.append("Concepto,Periodo,Vence,Monto,Pagado,Estado\n");
        for (var c : st.charges()) {
            sb.append(csv(c.concept())).append(',')
              .append(csv(c.period())).append(',')
              .append(c.dueDate()).append(',')
              .append(c.amount()).append(',')
              .append(c.paidAmount()).append(',')
              .append(c.status()).append('\n');
        }
        sb.append('\n');

        sb.append("Pagos\n");
        sb.append("Fecha,Monto,Método,Estado de conciliación\n");
        for (var p : st.payments()) {
            sb.append(p.reportedAt()).append(',')
              .append(p.amount()).append(',')
              .append(p.method()).append(',')
              .append(p.reconciliationStatus()).append('\n');
        }
        return sb.toString();
    }

    private static String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}

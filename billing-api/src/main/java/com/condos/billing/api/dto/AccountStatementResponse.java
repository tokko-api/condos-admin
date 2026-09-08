package com.condos.billing.api.dto;

import java.math.BigDecimal;
import java.util.List;

/** Estado de cuenta de una unidad (RN-PAG-05): cargos y pagos, con saldo actual. */
public record AccountStatementResponse(
        String unitId,
        String boardId,
        BigDecimal totalCharged,
        BigDecimal totalPaid,
        BigDecimal balance, // totalCharged - totalPaid
        BigDecimal creditBalance, // saldo a favor explícito (excedentes de pagos, ver UnitCredit)
        List<ChargeResponse> charges,
        List<PaymentResponse> payments
) {}

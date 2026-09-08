package com.condos.billing.service;

import com.condos.billing.model.UnitCredit;
import com.condos.billing.repository.UnitCreditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

/** Saldo a favor por unidad (ver UnitCredit). */
@Service
@RequiredArgsConstructor
public class CreditService {

    private final UnitCreditRepository repo;

    public BigDecimal getBalance(String unitId) {
        return repo.findById(unitId).map(UnitCredit::getBalance).orElse(BigDecimal.ZERO);
    }

    /** Suma `amount` (puede ser negativo para consumir crédito) al saldo a favor de la unidad. */
    public void addCredit(String orgId, String boardId, String unitId, BigDecimal amount) {
        if (amount == null || amount.signum() == 0) return;
        UnitCredit c = repo.findById(unitId).orElseGet(() -> UnitCredit.builder()
                .unitId(unitId)
                .orgId(orgId)
                .boardId(boardId)
                .balance(BigDecimal.ZERO)
                .build());
        c.setBalance((c.getBalance() != null ? c.getBalance() : BigDecimal.ZERO).add(amount));
        c.setUpdatedAt(Instant.now());
        repo.save(c);
    }
}

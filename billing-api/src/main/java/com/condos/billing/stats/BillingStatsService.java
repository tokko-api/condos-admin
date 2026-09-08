package com.condos.billing.stats;

import com.condos.billing.api.dto.BoardCollectionRes;
import com.condos.billing.api.dto.BoardExpenseRes;
import com.condos.billing.api.dto.ExpenseCategoryBreakdownRes;
import com.condos.billing.model.Charge;
import com.condos.billing.model.ChargeStatus;
import com.condos.billing.model.Expense;
import com.condos.billing.model.ExpenseStatus;
import com.condos.billing.repository.ChargeRepository;
import com.condos.billing.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cobranza (cuotas de mantenimiento) agregada por condominio (boardId),
 * a partir de los `charges` generados por unidad (RN-PAG).
 *
 * "collected" suma `Charge.paidAmount` (lo que realmente se ha aplicado a
 * cada cargo vía pagos conciliados, ver PaymentServiceImpl.reconcile), no
 * solo el monto completo de los cargos en status PAID — así un cargo
 * PARTIALLY_PAID también aporta lo que de verdad se cobró.
 */
@Service
@RequiredArgsConstructor
public class BillingStatsService {

    private final ChargeRepository chargeRepo;
    private final ExpenseRepository expenseRepo;

    public List<BoardCollectionRes> collectionByBoard(String orgId, String period) {
        String effectivePeriod = (period != null && !period.isBlank())
                ? period
                : YearMonth.now(ZoneOffset.UTC).toString(); // "2026-08"

        List<Charge> charges = chargeRepo.findByOrgIdAndPeriod(orgId, effectivePeriod);

        Map<String, BigDecimal[]> acc = new HashMap<>(); // boardId -> [billed, collected]
        for (Charge c : charges) {
            if (c.getStatus() == ChargeStatus.CANCELLED) continue; // no cuenta cargos cancelados
            BigDecimal amount = c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO;
            BigDecimal paid = c.getPaidAmount() != null ? c.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal[] arr = acc.computeIfAbsent(c.getBoardId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            arr[0] = arr[0].add(amount);
            arr[1] = arr[1].add(paid.min(amount)); // nunca cuenta de más aunque paidAmount esté corrupto
        }

        return acc.entrySet().stream()
                .map(e -> {
                    BigDecimal billed = e.getValue()[0];
                    BigDecimal collected = e.getValue()[1];
                    double pct = billed.signum() == 0
                            ? 0.0
                            : collected.multiply(BigDecimal.valueOf(100))
                                .divide(billed, 1, RoundingMode.HALF_UP)
                                .doubleValue();
                    return new BoardCollectionRes(e.getKey(), billed, collected, pct);
                })
                .sorted(Comparator.comparing(BoardCollectionRes::boardId))
                .toList();
    }

    /** Egresos del periodo agrupados por condominio (boardId), para el estado financiero. */
    public List<BoardExpenseRes> expensesByBoard(String orgId, String period) {
        String effectivePeriod = (period != null && !period.isBlank())
                ? period
                : YearMonth.now(ZoneOffset.UTC).toString();

        List<Expense> expenses = expenseRepo.findByOrgIdAndPeriod(orgId, effectivePeriod);

        Map<String, BigDecimal> acc = new HashMap<>();
        for (Expense e : expenses) {
            if (e.getStatus() == ExpenseStatus.CANCELLED) continue;
            BigDecimal amount = e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO;
            acc.merge(e.getBoardId(), amount, BigDecimal::add);
        }

        return acc.entrySet().stream()
                .map(en -> new BoardExpenseRes(en.getKey(), en.getValue()))
                .sorted(Comparator.comparing(BoardExpenseRes::boardId))
                .toList();
    }

    /** Desglose de egresos por categoría de una colonia (boardId) en un periodo. */
    public List<ExpenseCategoryBreakdownRes> expensesByCategory(String orgId, String boardId, String period) {
        String effectivePeriod = (period != null && !period.isBlank())
                ? period
                : YearMonth.now(ZoneOffset.UTC).toString();

        List<Expense> expenses = expenseRepo.findByOrgIdAndPeriod(orgId, effectivePeriod);

        Map<com.condos.billing.model.ExpenseCategory, BigDecimal> acc = new EnumMap<>(com.condos.billing.model.ExpenseCategory.class);
        for (Expense e : expenses) {
            if (e.getStatus() == ExpenseStatus.CANCELLED) continue;
            if (boardId != null && !boardId.equals(e.getBoardId())) continue;
            BigDecimal amount = e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO;
            acc.merge(e.getCategory(), amount, BigDecimal::add);
        }

        return acc.entrySet().stream()
                .map(en -> new ExpenseCategoryBreakdownRes(en.getKey(), en.getValue()))
                .sorted(Comparator.comparing(en -> en.category().name()))
                .toList();
    }
}

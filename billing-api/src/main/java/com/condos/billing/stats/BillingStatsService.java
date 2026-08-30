package com.condos.billing.stats;

import com.condos.billing.api.dto.BoardCollectionRes;
import com.condos.billing.model.Charge;
import com.condos.billing.model.ChargeStatus;
import com.condos.billing.repository.ChargeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cobranza (cuotas de mantenimiento) agregada por condominio (boardId),
 * a partir de los `charges` generados por unidad (RN-PAG).
 *
 * NOTA (misma simplificación documentada en PaymentServiceImpl.reconcile):
 * hoy un cargo pasa a PAID íntegro al conciliar un pago, sin prorrateo
 * parcial. Por eso "collected" se calcula como la suma de los charges en
 * status PAID del periodo, no sumando payments directamente — así el
 * agregado es consistente con lo que ya muestra el estado de cuenta por
 * unidad (AccountStatementService).
 */
@Service
@RequiredArgsConstructor
public class BillingStatsService {

    private final ChargeRepository chargeRepo;

    public List<BoardCollectionRes> collectionByBoard(String orgId, String period) {
        String effectivePeriod = (period != null && !period.isBlank())
                ? period
                : YearMonth.now(ZoneOffset.UTC).toString(); // "2026-08"

        List<Charge> charges = chargeRepo.findByOrgIdAndPeriod(orgId, effectivePeriod);

        Map<String, BigDecimal[]> acc = new HashMap<>(); // boardId -> [billed, collected]
        for (Charge c : charges) {
            if (c.getStatus() == ChargeStatus.CANCELLED) continue; // no cuenta cargos cancelados
            BigDecimal amount = c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO;
            BigDecimal[] arr = acc.computeIfAbsent(c.getBoardId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            arr[0] = arr[0].add(amount);
            if (c.getStatus() == ChargeStatus.PAID) {
                arr[1] = arr[1].add(amount);
            }
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
}

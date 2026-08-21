package com.condos.billing.service;

import com.condos.billing.api.dto.CreateExtraordinaryChargeRequest;
import com.condos.billing.model.Charge;
import com.condos.billing.model.ChargeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

public interface ChargeService {

    /** Genera (si no existen ya) los cargos regulares del periodo para todas las unidades activas del board. */
    List<Charge> generateForPeriod(String orgId, String boardId, String period, String bearerToken);

    List<Charge> createExtraordinary(String orgId, CreateExtraordinaryChargeRequest req, String bearerToken);

    Optional<Charge> get(String id);

    Page<Charge> listByUnit(String unitId, ChargeStatus status, int page, int size, String sortBy, Sort.Direction dir);

    Page<Charge> listByBoard(String boardId, ChargeStatus status, int page, int size, String sortBy, Sort.Direction dir);
}

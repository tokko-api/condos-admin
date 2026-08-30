package com.condos.billing.repository;

import com.condos.billing.model.Charge;
import com.condos.billing.model.ChargeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ChargeRepository extends MongoRepository<Charge, String> {
    Page<Charge> findByUnitId(String unitId, Pageable pageable);
    Page<Charge> findByUnitIdAndStatus(String unitId, ChargeStatus status, Pageable pageable);

    Page<Charge> findByBoardId(String boardId, Pageable pageable);
    Page<Charge> findByBoardIdAndStatus(String boardId, ChargeStatus status, Pageable pageable);

    Optional<Charge> findByUnitIdAndFeeScheduleIdAndPeriod(String unitId, String feeScheduleId, String period);

    java.util.List<Charge> findByOrgIdAndPeriod(String orgId, String period);
}

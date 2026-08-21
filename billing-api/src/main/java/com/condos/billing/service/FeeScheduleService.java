package com.condos.billing.service;

import com.condos.billing.model.FeeFrequency;
import com.condos.billing.model.FeeSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.Optional;

public interface FeeScheduleService {

    FeeSchedule create(String orgId, String boardId, String name, BigDecimal amount, String currency,
                        FeeFrequency frequency, Integer dueDayOfPeriod, String createdBy);

    Optional<FeeSchedule> get(String id);

    FeeSchedule update(String id, String name, BigDecimal amount, String currency, FeeFrequency frequency,
                        Integer dueDayOfPeriod, Boolean active);

    Page<FeeSchedule> list(String boardId, int page, int size, String sortBy, Sort.Direction dir);
}

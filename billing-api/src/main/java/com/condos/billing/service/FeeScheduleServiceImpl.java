package com.condos.billing.service;

import com.condos.billing.model.FeeFrequency;
import com.condos.billing.model.FeeSchedule;
import com.condos.billing.repository.FeeScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FeeScheduleServiceImpl implements FeeScheduleService {

    private final FeeScheduleRepository repo;

    @Override
    public FeeSchedule create(String orgId, String boardId, String name, BigDecimal amount, String currency,
                               FeeFrequency frequency, Integer dueDayOfPeriod, String createdBy) {
        FeeSchedule s = FeeSchedule.newSchedule(
                orgId, boardId, name, amount,
                (currency == null || currency.isBlank()) ? "MXN" : currency,
                frequency,
                dueDayOfPeriod == null ? 5 : dueDayOfPeriod,
                createdBy
        );
        return repo.save(s);
    }

    @Override
    public Optional<FeeSchedule> get(String id) {
        return repo.findById(id);
    }

    @Override
    public FeeSchedule update(String id, String name, BigDecimal amount, String currency, FeeFrequency frequency,
                               Integer dueDayOfPeriod, Boolean active) {
        FeeSchedule s = repo.findById(id).orElseThrow(() -> notFound(id));
        if (name != null) s.setName(name);
        if (amount != null) s.setAmount(amount);
        if (currency != null) s.setCurrency(currency);
        if (frequency != null) s.setFrequency(frequency);
        if (dueDayOfPeriod != null) s.setDueDayOfPeriod(dueDayOfPeriod);
        if (active != null) s.setActive(active);
        s.setUpdatedAt(Instant.now());
        return repo.save(s);
    }

    @Override
    public Page<FeeSchedule> list(String boardId, int page, int size, String sortBy, Sort.Direction dir) {
        var pageable = PageRequest.of(page, size, Sort.by(dir, sortBy == null || sortBy.isBlank() ? "createdAt" : sortBy));
        return repo.findByBoardId(boardId, pageable);
    }

    private ResponseStatusException notFound(String id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "fee schedule not found: " + id);
    }
}

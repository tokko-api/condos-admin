package com.condos.billing.service;

import com.condos.billing.api.dto.CreateExtraordinaryChargeRequest;
import com.condos.billing.model.*;
import com.condos.billing.repository.ChargeRepository;
import com.condos.billing.repository.FeeScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChargeServiceImpl implements ChargeService {

    private final ChargeRepository charges;
    private final FeeScheduleRepository schedules;
    private final BoardApiClient boardApiClient;

    @Override
    public List<Charge> generateForPeriod(String orgId, String boardId, String period, String bearerToken) {
        List<FeeSchedule> active = schedules.findByBoardIdAndActiveTrue(boardId);
        if (active.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No hay cuotas activas (FeeSchedule) configuradas para boardId=" + boardId);
        }

        var units = boardApiClient.listActiveUnitIds(boardId, bearerToken);
        if (units.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No hay unidades activas en boardId=" + boardId + " (o board-api no las devolvió)");
        }

        List<Charge> created = new ArrayList<>();
        Instant dueDate = dueDateFor(period, active.get(0).getDueDayOfPeriod());

        for (FeeSchedule schedule : active) {
            for (var unit : units) {
                boolean exists = charges
                        .findByUnitIdAndFeeScheduleIdAndPeriod(unit.id(), schedule.getId(), period)
                        .isPresent();
                if (exists) continue;

                Charge c = Charge.newCharge(
                        orgId, boardId, unit.id(), schedule.getId(),
                        ChargeType.REGULAR, schedule.getName(), schedule.getAmount(), period,
                        dueDateFor(period, schedule.getDueDayOfPeriod()), null
                );
                try {
                    created.add(charges.save(c));
                } catch (DuplicateKeyException ignored) {
                    // se generó en una corrida concurrente / carrera; no es un error real
                }
            }
        }
        return created;
    }

    @Override
    public List<Charge> createExtraordinary(String orgId, CreateExtraordinaryChargeRequest req, String bearerToken) {
        List<String> unitIds = req.unitIds();
        if (unitIds == null || unitIds.isEmpty()) {
            unitIds = boardApiClient.listActiveUnitIds(req.boardId(), bearerToken)
                    .stream().map(BoardApiClient.UnitRef::id).toList();
        }
        if (unitIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No hay unidades activas en boardId=" + req.boardId());
        }

        AssemblyRef assemblyRef = (req.actaNumber() != null || req.actaDate() != null || req.attachmentFileId() != null)
                ? AssemblyRef.builder()
                        .actaNumber(req.actaNumber())
                        .actaDate(req.actaDate())
                        .attachmentFileId(req.attachmentFileId())
                        .build()
                : null;

        Instant dueDate = req.dueDate() != null ? req.dueDate() : Instant.now().plusSeconds(15L * 24 * 3600);

        List<Charge> created = new ArrayList<>();
        for (String unitId : unitIds) {
            Charge c = Charge.newCharge(
                    orgId, req.boardId(), unitId, null,
                    ChargeType.EXTRAORDINARY, req.concept(), req.amount(), null, dueDate, assemblyRef
            );
            created.add(charges.save(c));
        }
        return created;
    }

    @Override
    public Optional<Charge> get(String id) {
        return charges.findById(id);
    }

    @Override
    public Page<Charge> listByUnit(String unitId, ChargeStatus status, int page, int size, String sortBy, Sort.Direction dir) {
        var pageable = PageRequest.of(page, size, Sort.by(dir, defaultSort(sortBy)));
        return status != null
                ? charges.findByUnitIdAndStatus(unitId, status, pageable)
                : charges.findByUnitId(unitId, pageable);
    }

    @Override
    public Page<Charge> listByBoard(String boardId, ChargeStatus status, int page, int size, String sortBy, Sort.Direction dir) {
        var pageable = PageRequest.of(page, size, Sort.by(dir, defaultSort(sortBy)));
        return status != null
                ? charges.findByBoardIdAndStatus(boardId, status, pageable)
                : charges.findByBoardId(boardId, pageable);
    }

    private String defaultSort(String sortBy) {
        return (sortBy == null || sortBy.isBlank()) ? "dueDate" : sortBy;
    }

    private Instant dueDateFor(String period, int dueDayOfPeriod) {
        // period = "yyyy-MM"
        var ym = period.split("-");
        int year = Integer.parseInt(ym[0]);
        int month = Integer.parseInt(ym[1]);
        int day = Math.min(dueDayOfPeriod, LocalDate.of(year, month, 1).lengthOfMonth());
        return LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}

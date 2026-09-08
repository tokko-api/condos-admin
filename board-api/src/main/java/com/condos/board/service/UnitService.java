package com.condos.board.service;

import com.condos.board.model.Unit;
import com.condos.board.model.UnitStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface UnitService {

    Unit create(String orgId, String boardId, String identifier, String ownerName,
                String residentUserId, BigDecimal coefficient, boolean committeeMember);

    Optional<Unit> get(String id);

    List<Unit> listMine(String residentUserId);

    Unit update(String id, String identifier, String ownerName, String residentUserId, BigDecimal coefficient,
                Boolean committeeMember);

    Unit changeStatus(String id, UnitStatus status);

    Page<Unit> list(String boardId, String q, boolean includeInactive,
                     int page, int size, String sortBy, Sort.Direction dir);
}

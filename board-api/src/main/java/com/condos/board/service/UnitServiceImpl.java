package com.condos.board.service;

import com.condos.board.model.Unit;
import com.condos.board.model.UnitStatus;
import com.condos.board.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UnitServiceImpl implements UnitService {

    private final UnitRepository repo;

    @Override
    public Unit create(String orgId, String boardId, String identifier, String ownerName,
                        String residentUserId, BigDecimal coefficient) {
        Unit u = Unit.newUnit(orgId, boardId, identifier, ownerName, residentUserId, coefficient);
        try {
            return repo.save(u);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una unidad con el identificador '" + identifier + "' en esta colonia");
        }
    }

    @Override
    public Optional<Unit> get(String id) {
        return repo.findById(id);
    }

    @Override
    public Unit update(String id, String identifier, String ownerName, String residentUserId, BigDecimal coefficient) {
        Unit u = repo.findById(id).orElseThrow(() -> notFound(id));

        if (StringUtils.hasText(identifier)) u.setIdentifier(identifier);
        if (ownerName != null) u.setOwnerName(ownerName);
        if (residentUserId != null) u.setResidentUserId(residentUserId);
        if (coefficient != null) u.setCoefficient(coefficient);

        u.setUpdatedAt(Instant.now());
        try {
            return repo.save(u);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una unidad con el identificador '" + identifier + "' en esta colonia");
        }
    }

    @Override
    public Unit changeStatus(String id, UnitStatus status) {
        Unit u = repo.findById(id).orElseThrow(() -> notFound(id));
        u.setStatus(status);
        u.setUpdatedAt(Instant.now());
        return repo.save(u);
    }

    @Override
    public Page<Unit> list(String boardId, String q, boolean includeInactive,
                            int page, int size, String sortBy, Sort.Direction dir) {
        var pageable = PageRequest.of(page, size, Sort.by(dir, defaultSort(sortBy)));
        boolean hasQuery = StringUtils.hasText(q);
        String regex = hasQuery ? ".*" + q + ".*" : null;

        if (includeInactive) {
            return hasQuery
                    ? repo.findByBoardIdAndIdentifierRegexIgnoreCase(boardId, regex, pageable)
                    : repo.findByBoardId(boardId, pageable);
        }
        return hasQuery
                ? repo.findByBoardIdAndStatusAndIdentifierRegexIgnoreCase(boardId, UnitStatus.ACTIVE, regex, pageable)
                : repo.findByBoardIdAndStatus(boardId, UnitStatus.ACTIVE, pageable);
    }

    private String defaultSort(String sortBy) {
        return (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
    }

    private ResponseStatusException notFound(String id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "unit not found: " + id);
    }
}

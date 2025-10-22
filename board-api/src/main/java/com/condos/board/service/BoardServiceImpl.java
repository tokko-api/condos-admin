package com.condos.board.service;

import com.condos.board.model.Board;
import com.condos.board.model.BoardStatus;
import com.condos.board.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private final BoardRepository repo;

    @Override
    public Board create(String orgId, String name, String description) {
        var b = new Board();
        b.orgId = orgId;
        b.name = name;
        b.description = description;
        b.status = BoardStatus.ACTIVE;
        b.createdAt = Instant.now();
        b.updatedAt = Instant.now();
        return repo.save(b);
    }

    @Override
    public Optional<Board> get(String id) {
        return repo.findById(id);
    }

    @Override
    public Board update(String id, String name, String description) {
        var b = repo.findById(id).orElseThrow(() -> notFound(id));
        if (name != null) b.name = name;
        if (description != null) b.description = description;
        b.updatedAt = Instant.now();
        return repo.save(b);
    }

    @Override
    public Board archive(String id) {
        var b = repo.findById(id).orElseThrow(() -> notFound(id));
        b.status = BoardStatus.ARCHIVED;
        b.updatedAt =Instant.now();
        return repo.save(b);
    }

    @Override
    public Board activate(String id) {
        var b = repo.findById(id).orElseThrow(() -> notFound(id));
        b.status = BoardStatus.ACTIVE;
        b.updatedAt = Instant.now();
        return repo.save(b);
    }

    @Override
    public Page<Board> list(String orgId, String q, int page, int size, String sortBy, Sort.Direction dir) {
        var pageable = PageRequest.of(page, size, Sort.by(dir, defaultSort(sortBy)));
        var pageData = repo.findByOrgId(orgId, pageable);

        if (!StringUtils.hasText(q)) return pageData;

        var filtered = pageData.getContent().stream()
                .filter(b -> contains(b.name, q) || contains(b.description, q))
                .toList();

        return toPage(filtered, pageable, filtered.size());
    }

    @Override
    public Page<Board> listActive(String orgId, String q, int page, int size, String sortBy, Sort.Direction dir) {
        var pageable = PageRequest.of(page, size, Sort.by(dir, defaultSort(sortBy)));
        var pageData = repo.findByOrgIdAndStatusNot(orgId, BoardStatus.ARCHIVED, pageable);

        if (!StringUtils.hasText(q)) return pageData;

        var filtered = pageData.getContent().stream()
                .filter(b -> contains(b.name, q) || contains(b.description, q))
                .toList();

        return toPage(filtered, pageable, filtered.size());
    }

    // -------- helpers --------

    private RuntimeException notFound(String id) {
        return new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "board not found: " + id
        );
    }

    private boolean contains(String s, String q) {
        return s != null && q != null && s.toLowerCase().contains(q.toLowerCase());
    }

    private String defaultSort(String sortBy) {
        return (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
    }

    private <T> Page<T> toPage(List<T> data, Pageable pageable, long total) {
        return new PageImpl<>(data, pageable, total);
    }
}
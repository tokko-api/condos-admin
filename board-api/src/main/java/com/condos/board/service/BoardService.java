package com.condos.board.service;

import com.condos.board.model.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.Optional;

public interface BoardService {
    Board create(String orgId, String name, String description);
    Optional<Board> get(String id);
    Board update(String id, String name, String description);
    Board archive(String id);
    Board activate(String id);

    Page<Board> list(String orgId, String q, int page, int size, String sortBy, Sort.Direction dir);
    Page<Board> listActive(String orgId, String q, int page, int size, String sortBy, Sort.Direction dir);
}
package com.condos.board.repository;

import com.condos.board.model.Board;
import com.condos.board.model.BoardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BoardRepository extends MongoRepository<Board, String> {
    Page<Board> findByOrgId(String orgId, Pageable pageable);
    Page<Board> findByOrgIdAndStatusNot(String orgId, BoardStatus status, Pageable pageable);
}
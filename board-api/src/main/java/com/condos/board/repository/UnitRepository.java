package com.condos.board.repository;

import com.condos.board.model.Unit;
import com.condos.board.model.UnitStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UnitRepository extends MongoRepository<Unit, String> {

    Page<Unit> findByBoardId(String boardId, Pageable pageable);

    Page<Unit> findByBoardIdAndStatus(String boardId, UnitStatus status, Pageable pageable);

    Page<Unit> findByBoardIdAndIdentifierRegexIgnoreCase(String boardId, String q, Pageable pageable);

    Page<Unit> findByBoardIdAndStatusAndIdentifierRegexIgnoreCase(
            String boardId, UnitStatus status, String q, Pageable pageable);

    long countByBoardIdAndStatus(String boardId, UnitStatus status);
}

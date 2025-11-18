package com.condos.board.api.dto;

import com.condos.board.model.Board;
import com.condos.board.model.BoardStatus;

import java.time.Instant;

public record BoardResponse(
        String id,
        String name,
        String orgId,
        String description,
        Instant createdAt,
        Instant updatedAt,
        BoardStatus status
) {
    public static BoardResponse from(Board b) {
        return new BoardResponse(
                b.id != null ? b.id.toString() : null, // ObjectId -> String
                b.name,
                b.orgId,
                b.description,
                b.createdAt,
                b.updatedAt,
                b.status
        );
    }
}
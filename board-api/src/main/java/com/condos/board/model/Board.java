package com.condos.board.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "boards")
public class Board {
    @Id
    public String id;
    public String orgId;
    public String name;
    public String description;
    public BoardStatus status = BoardStatus.ACTIVE;
    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();

}
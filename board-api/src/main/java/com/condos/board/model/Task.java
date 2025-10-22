package com.condos.board.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Document("tasks")
public class Task {
    @Id
    private String id;

    @Indexed
    private String boardId;

    @Indexed
    private String orgId;

    @Indexed
    private String title;

    private String description;
    private String assigneeId;     // opcional
    private Instant dueDate;        // ISO yyyy-MM-dd (string simple)
    private TaskStatus status;

    private Instant createdAt;
    private Instant updatedAt;

    public static Task newTask(String orgId, String boardId, String title, String description,
                               String assigneeId, Instant dueDate) {
        Instant now = Instant.now();

        return Task.builder()
                .orgId(orgId)
                .boardId(boardId)
                .title(title)
                .description(description)
                .assigneeId(assigneeId)
                .dueDate(dueDate)
                .status(TaskStatus.OPEN)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
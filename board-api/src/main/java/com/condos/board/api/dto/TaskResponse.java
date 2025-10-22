package com.condos.board.api.dto;

import com.condos.board.model.Task;
import com.condos.board.model.TaskStatus;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public record TaskResponse(
        String id,
        String boardId,
        String orgId,
        String title,
        String description,
        String assigneeId,
        String dueDate,
        TaskStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static TaskResponse from(Task t) {
        var fmtDate = DateTimeFormatter.ISO_LOCAL_DATE;
        var fmtTs   = DateTimeFormatter.ISO_INSTANT;
        String dueStr = t.getDueDate() == null
                ? null
                : fmtDate.format(t.getDueDate().atZone(ZoneOffset.UTC).toLocalDate());
        return new TaskResponse(
                t.getId(), t.getBoardId(), t.getOrgId(), t.getTitle(),
                t.getDescription(), t.getAssigneeId(), dueStr,
                t.getStatus(), t.getCreatedAt(), t.getUpdatedAt()
        );
    }
}
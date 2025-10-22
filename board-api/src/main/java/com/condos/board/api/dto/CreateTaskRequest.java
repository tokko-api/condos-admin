package com.condos.board.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTaskRequest(
        @NotBlank String title,
        String description,
        String assigneeId,
        String dueDate
) {}
package com.condos.board.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateTaskRequest(
        @NotBlank String title,
        String description,
        String assigneeId,
        String dueDate
) {}
package com.condos.board.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record UpdateTaskRequest(
        @NotBlank String title,
        String description,
        String assigneeId,
        Instant dueDate
) {}
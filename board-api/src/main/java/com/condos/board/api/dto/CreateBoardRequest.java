package com.condos.board.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateBoardRequest(
        @NotBlank String orgId,
        @NotBlank String name,
        String description
) {}
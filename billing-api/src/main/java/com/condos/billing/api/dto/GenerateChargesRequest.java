package com.condos.billing.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Genera los cargos regulares del periodo indicado para todas las unidades activas de la colonia. */
public record GenerateChargesRequest(
        @NotBlank String boardId,
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}") String period // "2026-08"
) {}

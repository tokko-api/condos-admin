package com.condos.billing.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Crea una cuota extraordinaria (RN-PAG-02). Si unitIds viene vacío/null,
 * se aplica a todas las unidades activas de la colonia.
 */
public record CreateExtraordinaryChargeRequest(
        @NotBlank String boardId,
        List<String> unitIds,
        @NotBlank String concept,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        Instant dueDate,
        String actaNumber,
        Instant actaDate,
        String attachmentFileId
) {}

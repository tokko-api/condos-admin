package com.condos.billing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Respaldo documental de asamblea para cuotas extraordinarias (RN-PAG-02, RN-GEN-04). */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AssemblyRef {
    private String actaNumber;
    private Instant actaDate;
    private String attachmentFileId; // referencia a un archivo subido vía board-api/files
}

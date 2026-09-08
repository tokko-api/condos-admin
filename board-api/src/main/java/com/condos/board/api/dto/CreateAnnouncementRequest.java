package com.condos.board.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAnnouncementRequest(
        @NotBlank String title,
        String body,                    // opcional si viene un adjunto
        String attachmentFileId,        // key en /files, ya subido por el cliente
        String attachmentFileName,
        String attachmentContentType
) {}

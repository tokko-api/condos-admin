package com.condos.board.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** Un comunicado publicado por el personal de una colonia para sus condóminos. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Document("announcements")
public class Announcement {
    @Id
    private String id;

    @Indexed
    private String boardId;

    @Indexed
    private String orgId;

    private String title;
    private String body; // opcional si el comunicado es solo un archivo adjunto

    // Adjunto opcional (imagen, PDF, Word, etc.) — mismo esquema de "sube
    // primero al bucket de /files, luego guarda la referencia" que ya usa
    // el comprobante de un pago (ver PaymentController.receiptFileId).
    private String attachmentFileId;
    private String attachmentFileName;
    private String attachmentContentType;

    private String publishedBy; // userId (staff) que lo publicó

    private AnnouncementStatus status;

    private Instant createdAt;
    private Instant updatedAt;

    public static Announcement newAnnouncement(String orgId, String boardId, String title, String body,
                                                String attachmentFileId, String attachmentFileName,
                                                String attachmentContentType, String publishedBy) {
        Instant now = Instant.now();
        return Announcement.builder()
                .orgId(orgId)
                .boardId(boardId)
                .title(title)
                .body(body)
                .attachmentFileId(attachmentFileId)
                .attachmentFileName(attachmentFileName)
                .attachmentContentType(attachmentContentType)
                .publishedBy(publishedBy)
                .status(AnnouncementStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}

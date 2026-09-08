package com.condos.board.api.dto;

import com.condos.board.model.Announcement;
import com.condos.board.model.AnnouncementStatus;

import java.time.Instant;

public record AnnouncementResponse(
        String id,
        String boardId,
        String orgId,
        String title,
        String body,
        String attachmentFileId,
        String attachmentFileName,
        String attachmentContentType,
        String publishedBy,
        AnnouncementStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static AnnouncementResponse from(Announcement a) {
        return new AnnouncementResponse(
                a.getId(), a.getBoardId(), a.getOrgId(), a.getTitle(), a.getBody(),
                a.getAttachmentFileId(), a.getAttachmentFileName(), a.getAttachmentContentType(),
                a.getPublishedBy(), a.getStatus(), a.getCreatedAt(), a.getUpdatedAt()
        );
    }
}

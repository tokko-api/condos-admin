package com.condos.board.service;

import com.condos.board.model.Announcement;
import com.condos.board.model.AnnouncementStatus;
import com.condos.board.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository repo;

    public Announcement create(String orgId, String boardId, String title, String body,
                                String attachmentFileId, String attachmentFileName, String attachmentContentType,
                                String publishedBy) {
        if (!StringUtils.hasText(body) && !StringUtils.hasText(attachmentFileId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El comunicado necesita un mensaje o un archivo adjunto.");
        }
        Announcement a = Announcement.newAnnouncement(orgId, boardId, title, body,
                attachmentFileId, attachmentFileName, attachmentContentType, publishedBy);
        return repo.save(a);
    }

    public Optional<Announcement> get(String id) {
        return repo.findById(id);
    }

    public List<Announcement> listByBoard(String boardId, boolean includeArchived) {
        return includeArchived
                ? repo.findByBoardIdOrderByCreatedAtDesc(boardId)
                : repo.findByBoardIdAndStatusOrderByCreatedAtDesc(boardId, AnnouncementStatus.ACTIVE);
    }

    public Announcement update(String id, String title, String body,
                                String attachmentFileId, String attachmentFileName, String attachmentContentType) {
        Announcement a = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "announcement not found: " + id));
        if (!StringUtils.hasText(body) && !StringUtils.hasText(attachmentFileId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El comunicado necesita un mensaje o un archivo adjunto.");
        }
        a.setTitle(title);
        a.setBody(body);
        a.setAttachmentFileId(attachmentFileId);
        a.setAttachmentFileName(attachmentFileName);
        a.setAttachmentContentType(attachmentContentType);
        a.setUpdatedAt(Instant.now());
        return repo.save(a);
    }

    public Announcement archive(String id) {
        Announcement a = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "announcement not found: " + id));
        a.setStatus(AnnouncementStatus.ARCHIVED);
        a.setUpdatedAt(Instant.now());
        return repo.save(a);
    }
}

package com.condos.board.api;

import com.condos.board.api.dto.AnnouncementResponse;
import com.condos.board.api.dto.CreateAnnouncementRequest;
import com.condos.board.service.AnnouncementService;
import com.condos.board.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/condos/api/board")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcements;
    private final BoardService boards;

    // ======== CREATE (publicar comunicado) — staff de la colonia ========
    @PostMapping("/boards/{boardId}/announcements")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToBoard(authentication, #boardId, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})
    """)
    public AnnouncementResponse create(@PathVariable String boardId,
                                        @Valid @RequestBody CreateAnnouncementRequest req,
                                        Authentication authentication) {
        var board = boards.get(boardId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var a = announcements.create(board.orgId, boardId, req.title(), req.body(),
                req.attachmentFileId(), req.attachmentFileName(), req.attachmentContentType(),
                authentication.getName());
        return AnnouncementResponse.from(a);
    }

    // ======== LIST por colonia: staff o residente ========
    @GetMapping("/boards/{boardId}/announcements")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToBoard(authentication, #boardId, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'}) or
        @jwtAuth.isResidentOfBoard(authentication, #boardId)
    """)
    public List<AnnouncementResponse> list(@PathVariable String boardId,
                                            @RequestParam(defaultValue = "false") boolean includeArchived) {
        return announcements.listByBoard(boardId, includeArchived).stream().map(AnnouncementResponse::from).toList();
    }

    // ======== EDITAR (corregir título, mensaje o adjunto) ========
    @PatchMapping("/announcements/{id}")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToAnnouncement(authentication, #id, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})
    """)
    public AnnouncementResponse update(@PathVariable String id,
                                        @Valid @RequestBody CreateAnnouncementRequest req) {
        var a = announcements.update(id, req.title(), req.body(),
                req.attachmentFileId(), req.attachmentFileName(), req.attachmentContentType());
        return AnnouncementResponse.from(a);
    }

    // ======== ARCHIVAR (quitarlo de la vista de los condóminos) ========
    @PatchMapping("/announcements/{id}/archive")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToAnnouncement(authentication, #id, {'ADMINISTRADOR','SUPERVISOR'})
    """)
    public AnnouncementResponse archive(@PathVariable String id) {
        var a = announcements.archive(id);
        return AnnouncementResponse.from(a);
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public java.util.Map<String, Object> onNotFound(RuntimeException ex) {
        return java.util.Map.of("error", "not_found", "message", ex.getMessage());
    }
}

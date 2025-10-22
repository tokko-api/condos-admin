package com.condos.board.api;

import com.condos.board.api.dto.*;
import com.condos.board.service.AttachService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/condos/api/board/{boardId}/tasks/{taskId}")
@RequiredArgsConstructor
public class TaskCommentController {

    private final AttachService service;

    // ======== Presign / Complete (adjuntos) ========

    @PostMapping("/attachments/presign")
    @PreAuthorize("""
      @jwtAuth.isSuperadmin(authentication) or
      @jwtAuth.hasAccessToBoard(authentication, #boardId, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})
    """)
    public Mono<PresignResponse> presign(@PathVariable String boardId,
                                         @PathVariable String taskId,
                                         @RequestBody PresignRequest req) {
        return service.presign(boardId, taskId, req);
    }

    @PostMapping("/attachments/complete")
    @PreAuthorize("""
      @jwtAuth.isSuperadmin(authentication) or
      @jwtAuth.hasAccessToBoard(authentication, #boardId, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})
    """)
    public Mono<AttachmentDto> complete(@PathVariable String boardId,
                                        @PathVariable String taskId,
                                        @RequestBody CompleteUploadDto req,
                                        Authentication authentication) {
        String user = authentication != null ? authentication.getName() : "system";
        return service.complete(boardId, taskId, req, user);
    }

    @GetMapping("/attachments")
    @PreAuthorize("""
      @jwtAuth.isSuperadmin(authentication) or
      @jwtAuth.hasAccessToBoard(authentication, #boardId, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})
    """)
    public Flux<AttachmentDto> listAttachments(@PathVariable String boardId,
                                               @PathVariable String taskId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        // Re-firma GET y regresa URL fresca
        return service.listAttachments(taskId, page, size);
    }

    // ======== Comentarios (independientes de adjuntos) ========

    @PostMapping("/comments")
    @PreAuthorize("""
      @jwtAuth.isSuperadmin(authentication) or
      @jwtAuth.hasAccessToBoard(authentication, #boardId, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})
    """)
    public Mono<TaskCommentDto> addComment(@PathVariable String boardId,
                                           @PathVariable String taskId,
                                           @RequestBody CreateCommentDto req) {
        return service.addComment(taskId, req);
    }

    @GetMapping("/comments")
    @PreAuthorize("""
      @jwtAuth.isSuperadmin(authentication) or
      @jwtAuth.hasAccessToBoard(authentication, #boardId, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})
    """)
    public Flux<TaskCommentDto> listComments(@PathVariable String boardId,
                                             @PathVariable String taskId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return service.listComments(taskId, page, size);
    }
}
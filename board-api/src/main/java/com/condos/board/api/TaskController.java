package com.condos.board.api;

import com.condos.board.api.dto.CreateTaskRequest;
import com.condos.board.api.dto.StatusReq;
import com.condos.board.api.dto.TaskResponse;
import com.condos.board.api.dto.UpdateTaskRequest;
import com.condos.board.model.TaskStatus;
import com.condos.board.security.JwtAuth;
import com.condos.board.service.BoardService;
import com.condos.board.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/condos/api/board")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService tasks;
    private final BoardService boards; // para resolver orgId del board
    private final JwtAuth jwtAuth;

    // ======== CREATE ========
    @PostMapping("/boards/{boardId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToBoard(authentication, #boardId, {'ADMINISTRADOR','SUPERVISOR'})
    """)
    public TaskResponse create(@PathVariable String boardId,
                               @Valid @RequestBody CreateTaskRequest req) {
        // obtenemos orgId del board para guardar junto con la tarea
        var board = boards.get(boardId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var t = tasks.create(board.orgId, boardId, req.title(), req.description(), req.assigneeId(), req.dueDate());
        return TaskResponse.from(t);
    }

    // ======== LIST by board ========
    @GetMapping("/boards/{boardId}/tasks")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToBoard(authentication, #boardId, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})
    """)
    public Page<TaskResponse> list(@PathVariable String boardId,
                                   @RequestParam(required = false) String q,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   @RequestParam(defaultValue = "createdAt") String sortBy,
                                   @RequestParam(defaultValue = "DESC") Sort.Direction dir,
                                   @RequestParam(defaultValue = "false") boolean includeArchived) {
        var result = includeArchived
                ? tasks.list(boardId, q, page, size, sortBy, dir)
                : tasks.listActive(boardId, q, page, size, sortBy, dir);
        return result.map(TaskResponse::from);
    }

    // ======== READ ONE ========
    @GetMapping("/tasks/{id}")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToTask(authentication, #id, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})
    """)
    public TaskResponse get(@PathVariable String id) {
        var t = tasks.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return TaskResponse.from(t);
    }

    // ======== UPDATE ========
    @PutMapping("/tasks/{id}")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToTask(authentication, #id, {'ADMINISTRADOR','SUPERVISOR'})
    """)
    public TaskResponse update(@PathVariable String id, @Valid @RequestBody UpdateTaskRequest req) {
        var t = tasks.update(id, req.title(), req.description(), req.assigneeId(), req.dueDate());
        return TaskResponse.from(t);
    }

    // ======== CHANGE STATUS ========
    @PatchMapping("/tasks/{id}/status")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToTask(authentication, #id, {'ADMINISTRADOR','SUPERVISOR'})
    """)
    public TaskResponse changeStatus(@PathVariable String id, @RequestBody StatusReq req) {
        var t = tasks.changeStatus(id, req.status());
        return TaskResponse.from(t);
    }

    // ======== SOFT DELETE ========
    @DeleteMapping("/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToTask(authentication, #id, {'ADMINISTRADOR'})
    """)
    public void delete(@PathVariable String id) {
        tasks.delete(id);
    }

    // ======== Simple Handlers ========
    @ExceptionHandler(java.util.NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public java.util.Map<String, Object> onNotFound(RuntimeException ex) {
        return java.util.Map.of("error", "not_found", "message", ex.getMessage());
    }

    @GetMapping("/tasks") // <-- NUEVO
    @PreAuthorize("""
    @jwtAuth.hasRoleInOrg(authentication, #orgId,
      {'OPERATIVO','SUPERVISOR','ADMINISTRADOR','SUPERADMIN'})
""")
    public Page<TaskResponse> listByAssignee(
            @RequestParam String orgId,
            @RequestParam(defaultValue = "me") String assigneeId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false, defaultValue = "false") boolean overdue,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "dueDate") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction dir
    ) {
        // "me" -> id del usuario autenticado
        String effAssignee = "me".equalsIgnoreCase(assigneeId)
                ? SecurityContextHolder.getContext().getAuthentication().getName()                    // implementa userId() en JwtAuth, lee el sub del JWT
                : assigneeId;

        var result = tasks.listByAssignee(orgId, effAssignee, status, overdue, q, page, size, sortBy, dir);
        return result.map(TaskResponse::from);
    }
}
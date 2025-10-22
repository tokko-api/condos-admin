package com.condos.board.api;

import com.condos.board.model.BoardStatus;
import com.condos.board.service.BoardService;
import com.condos.board.api.dto.CreateBoardRequest;
import com.condos.board.api.dto.UpdateBoardRequest;
import com.condos.board.api.dto.BoardResponse;
import com.condos.board.security.JwtAuth;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/condos/api/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService service;
    private final JwtAuth jwtAuth;

    // ======== CREATE ========
    @PostMapping("/boards")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasRoleInOrg(authentication, #req.orgId, {'ADMINISTRADOR','SUPERVISOR'})
    """)
    public BoardResponse create(@Valid @RequestBody CreateBoardRequest req) {
        var b = service.create(req.orgId(), req.name(), req.description());
        return BoardResponse.from(b);
    }

    // ======== READ (LIST by org) ========
    @GetMapping("/boards")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasRoleInOrg(authentication, #orgId, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})
    """)
    public Page<BoardResponse> list(
            @RequestParam String orgId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction dir,
            @RequestParam(defaultValue = "false") boolean includeArchived
    ) {
        var result = includeArchived
                ? service.list(orgId, q, page, size, sortBy, dir)          // ALL
                : service.listActive(orgId, q, page, size, sortBy, dir);    // hide ARCHIVED
        return result.map(BoardResponse::from);
    }

    // ======== READ (ONE) ========
    @GetMapping("/boards/{id}")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToBoard(authentication, #id, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})
    """)
    public BoardResponse get(@PathVariable String id) {
        var b = service.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return BoardResponse.from(b);
    }

    // ======== UPDATE ========
    @PutMapping("/boards/{id}")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToBoard(authentication, #id, {'ADMINISTRADOR','SUPERVISOR'})
    """)
    public BoardResponse update(@PathVariable String id, @Valid @RequestBody UpdateBoardRequest req) {
        var b = service.update(id, req.name(), req.description());
        return BoardResponse.from(b);
    }

    // ======== CHANGE STATUS (ARCHIVE / ACTIVATE) ========
    @PatchMapping("/boards/{id}/status")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToBoard(authentication, #id, {'ADMINISTRADOR'})
    """)
    public BoardResponse changeStatus(@PathVariable String id,
                                      @RequestParam BoardStatus status) {
        var b = switch (status) {
            case ACTIVE -> service.activate(id);
            case ARCHIVED -> service.archive(id);
        };
        return BoardResponse.from(b);
    }

    // ======== SOFT DELETE (alias de ARCHIVE) ========
    @DeleteMapping("/boards/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToBoard(authentication, #id, {'ADMINISTRADOR'})
    """)
    public void delete(@PathVariable String id) {
        service.archive(id);
    }

    // ======== SIMPLE ERROR HANDLERS ========
    @ExceptionHandler(java.util.NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public java.util.Map<String, Object> onNotFound(RuntimeException ex) {
        return java.util.Map.of("error", "not_found", "message", ex.getMessage());
    }

    @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public java.util.Map<String, Object> onDup(Exception ex) {
        return java.util.Map.of("error", "duplicate", "message", ex.getMessage());
    }
}
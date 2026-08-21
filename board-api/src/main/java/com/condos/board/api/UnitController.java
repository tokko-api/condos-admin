package com.condos.board.api;

import com.condos.board.api.dto.CreateUnitRequest;
import com.condos.board.api.dto.UnitResponse;
import com.condos.board.api.dto.UnitStatusReq;
import com.condos.board.api.dto.UpdateUnitRequest;
import com.condos.board.security.JwtAuth;
import com.condos.board.service.BoardService;
import com.condos.board.service.UnitService;
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
public class UnitController {

    private final UnitService units;
    private final BoardService boards; // para resolver orgId del board al crear

    // ======== CREATE ========
    @PostMapping("/boards/{boardId}/units")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToBoard(authentication, #boardId, {'ADMINISTRADOR','SUPERVISOR'})
    """)
    public UnitResponse create(@PathVariable String boardId,
                                @Valid @RequestBody CreateUnitRequest req) {
        var board = boards.get(boardId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var u = units.create(board.orgId, boardId, req.identifier(), req.ownerName(),
                req.residentUserId(), req.coefficient());
        return UnitResponse.from(u);
    }

    // ======== LIST by board (colonia) ========
    @GetMapping("/boards/{boardId}/units")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToBoard(authentication, #boardId, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})
    """)
    public Page<UnitResponse> list(@PathVariable String boardId,
                                    @RequestParam(required = false) String q,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "50") int size,
                                    @RequestParam(defaultValue = "identifier") String sortBy,
                                    @RequestParam(defaultValue = "ASC") Sort.Direction dir,
                                    @RequestParam(defaultValue = "false") boolean includeInactive) {
        var result = units.list(boardId, q, includeInactive, page, size, sortBy, dir);
        return result.map(UnitResponse::from);
    }

    // ======== READ ONE ========
    @GetMapping("/units/{id}")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToUnit(authentication, #id, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})
    """)
    public UnitResponse get(@PathVariable String id) {
        var u = units.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return UnitResponse.from(u);
    }

    // ======== UPDATE ========
    @PutMapping("/units/{id}")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToUnit(authentication, #id, {'ADMINISTRADOR','SUPERVISOR'})
    """)
    public UnitResponse update(@PathVariable String id, @Valid @RequestBody UpdateUnitRequest req) {
        var u = units.update(id, req.identifier(), req.ownerName(), req.residentUserId(), req.coefficient());
        return UnitResponse.from(u);
    }

    // ======== CHANGE STATUS (alta/baja de casa) ========
    @PatchMapping("/units/{id}/status")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToUnit(authentication, #id, {'ADMINISTRADOR','SUPERVISOR'})
    """)
    public UnitResponse changeStatus(@PathVariable String id, @RequestBody UnitStatusReq req) {
        var u = units.changeStatus(id, req.status());
        return UnitResponse.from(u);
    }

    // ======== Simple Handlers ========
    @ExceptionHandler(java.util.NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public java.util.Map<String, Object> onNotFound(RuntimeException ex) {
        return java.util.Map.of("error", "not_found", "message", ex.getMessage());
    }
}

package com.condos.billing.api;

import com.condos.billing.api.dto.CreateExpenseRequest;
import com.condos.billing.api.dto.ExpenseResponse;
import com.condos.billing.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/condos/api/billing")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenses;

    @PostMapping("/boards/{boardId}/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasRoleInOrg(authentication, #orgId, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})
    """)
    public ExpenseResponse create(@PathVariable String boardId,
                                   @RequestParam String orgId,
                                   @Valid @RequestBody CreateExpenseRequest req,
                                   Authentication authentication) {
        var e = expenses.create(orgId, boardId, req.concept(), req.category(), req.providerName(),
                req.amount(), req.method(), req.expenseDate(), req.receiptFileId(), req.receiptFileName(),
                req.notes(), authentication.getName());
        return ExpenseResponse.from(e);
    }

    @GetMapping("/boards/{boardId}/expenses")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasRoleInOrg(authentication, #orgId, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})
    """)
    public Page<ExpenseResponse> listByBoard(@PathVariable String boardId,
                                              @RequestParam String orgId,
                                              @RequestParam(required = false) String period,
                                              @RequestParam(defaultValue = "false") boolean includeCancelled,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "50") int size,
                                              @RequestParam(defaultValue = "expenseDate") String sortBy,
                                              @RequestParam(defaultValue = "DESC") Sort.Direction dir) {
        return expenses.listByBoard(boardId, period, includeCancelled, page, size, sortBy, dir)
                .map(ExpenseResponse::from);
    }

    @GetMapping("/expenses/{id}")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToExpense(authentication, #id, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})
    """)
    public ExpenseResponse get(@PathVariable String id) {
        return expenses.get(id)
                .map(ExpenseResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "expense not found"));
    }

    @PatchMapping("/expenses/{id}/cancel")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasAccessToExpense(authentication, #id, {'ADMINISTRADOR','SUPERVISOR'})
    """)
    public ExpenseResponse cancel(@PathVariable String id) {
        return ExpenseResponse.from(expenses.cancel(id));
    }
}

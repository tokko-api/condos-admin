package com.condos.billing.api;

import com.condos.billing.api.dto.*;
import com.condos.billing.model.ReconciliationStatus;
import com.condos.billing.service.PaymentBulkImportService;
import com.condos.billing.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/condos/api/billing")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService payments;
    private final PaymentBulkImportService bulkImport;

    @PostMapping("/boards/{boardId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasRoleInOrg(authentication, #orgId, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})
    """)
    public PaymentResponse create(@PathVariable String boardId,
                                   @RequestParam String orgId,
                                   @Valid @RequestBody CreatePaymentRequest req) {
        var p = payments.create(orgId, boardId, req.unitId(), req.chargeIds(), req.amount(),
                req.method(), req.receiptFileId());
        return PaymentResponse.from(p);
    }

    @GetMapping("/payments")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasRoleInOrg(authentication, #orgId, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})
    """)
    public Page<PaymentResponse> list(@RequestParam String orgId,
                                       @RequestParam(required = false) String unitId,
                                       @RequestParam(required = false) String boardId,
                                       @RequestParam(required = false) ReconciliationStatus reconciliationStatus,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "50") int size,
                                       @RequestParam(defaultValue = "createdAt") String sortBy,
                                       @RequestParam(defaultValue = "DESC") Sort.Direction dir) {
        if (unitId == null && boardId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes indicar unitId o boardId");
        }
        var result = unitId != null
                ? payments.listByUnit(unitId, reconciliationStatus, page, size, sortBy, dir)
                : payments.listByBoard(boardId, reconciliationStatus, page, size, sortBy, dir);
        return result.map(PaymentResponse::from);
    }

    @GetMapping("/payments/{id}")
    @PreAuthorize("@jwtAuth.isSuperadmin(authentication) or @jwtAuth.hasAccessToPayment(authentication, #id, {'ADMINISTRADOR','SUPERVISOR','OPERATIVO'})")
    public PaymentResponse get(@PathVariable String id) {
        var p = payments.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return PaymentResponse.from(p);
    }

    @PostMapping("/payments/{id}/reconcile")
    @PreAuthorize("@jwtAuth.isSuperadmin(authentication) or @jwtAuth.hasAccessToPayment(authentication, #id, {'ADMINISTRADOR','SUPERVISOR'})")
    public PaymentResponse reconcile(@PathVariable String id,
                                      @Valid @RequestBody ReconcilePaymentRequest req,
                                      Authentication authentication) {
        var p = payments.reconcile(id, req.approve(), authentication.getName());
        return PaymentResponse.from(p);
    }

    /** Conciliar/rechazar varios pagos de un jalón (opción A del flujo masivo). */
    @PostMapping("/payments/reconcile-bulk")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasRoleInOrg(authentication, #orgId, {'ADMINISTRADOR','SUPERVISOR'})
    """)
    public List<BulkResultRow> reconcileBulk(@RequestParam String orgId,
                                              @Valid @RequestBody BulkReconcileRequest req,
                                              Authentication authentication) {
        return payments.bulkReconcile(orgId, req.paymentIds(), req.approve(), authentication.getName());
    }

    /** Importar pagos en lote desde Excel/CSV, ya parseado por el frontend (opción B del flujo masivo). */
    @PostMapping("/boards/{boardId}/payments/bulk-import")
    @PreAuthorize("""
        @jwtAuth.isSuperadmin(authentication) or
        @jwtAuth.hasRoleInOrg(authentication, #orgId, {'ADMINISTRADOR','SUPERVISOR'})
    """)
    public List<BulkResultRow> bulkImportPayments(@PathVariable String boardId,
                                                   @RequestParam String orgId,
                                                   @Valid @RequestBody BulkImportPaymentsRequest req,
                                                   HttpServletRequest http) {
        return bulkImport.importPayments(orgId, boardId, req.rows(), bearerToken(http));
    }

    private String bearerToken(HttpServletRequest http) {
        String h = http.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) return h.substring(7);
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing bearer token");
    }
}

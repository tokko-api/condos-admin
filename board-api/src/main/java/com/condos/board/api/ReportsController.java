package com.condos.board.api;

import com.condos.board.api.dto.BoardReportRes;
import com.condos.board.service.BoardReportsService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/condos/api/board/reports")
public class ReportsController {

    private final BoardReportsService svc;

    public ReportsController(BoardReportsService svc) {
        this.svc = svc;
    }

    /* ===================== OVERVIEW ===================== */
    @GetMapping
    public BoardReportRes get(
            @RequestParam String orgId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return svc.buildReport(orgId, from, to);
    }

    /* ===================== EXPORTS ===================== */

    @GetMapping(value = "/export/csv", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<ByteArrayResource> exportCsv(
            @RequestParam String orgId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        byte[] bytes = svc.buildCsv(orgId, from, to).getBytes(StandardCharsets.UTF_8);
        return attachment(bytes, "text/csv", fileName(orgId, from, to, "csv"));
    }

    @GetMapping(value = "/export/xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<ByteArrayResource> exportXlsx(
            @RequestParam String orgId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        byte[] bytes = svc.buildXlsx(orgId, from, to); // <- byte[]
        return attachment(bytes,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                fileName(orgId, from, to, "xlsx"));
    }

    @GetMapping(value = "/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<ByteArrayResource> exportPdf(
            @RequestParam String orgId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        byte[] bytes = svc.buildPdf(orgId, from, to); // <- byte[]
        return attachment(bytes, MediaType.APPLICATION_PDF_VALUE, fileName(orgId, from, to, "pdf"));
    }

    /* ===================== Helpers ===================== */

    private static ResponseEntity<ByteArrayResource> attachment(byte[] bytes, String contentType, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(bytes.length)
                .body(new ByteArrayResource(bytes));
    }

    private static String fileName(String orgId, LocalDate from, LocalDate to, String ext) {
        String base = "condos-report-" + safe(orgId) + "-" + from + "_" + to + "." + ext;
        return base.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String safe(String s) { return s == null ? "" : s.trim().replaceAll("\\s+", "-"); }
}
package com.condos.board.api;

import com.condos.board.service.BoardReportsService;
import com.condos.board.api.dto.BoardReportCsv;
import com.condos.board.api.dto.BoardReportRes;
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

    @GetMapping
    public BoardReportRes get(
            @RequestParam String orgId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return svc.buildReport(orgId, from, to);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam String orgId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        BoardReportCsv csv = svc.buildCsv(orgId, from, to);
        byte[] bytes = csv.content().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"condos-report-" + orgId + "-" + from + "_" + to + ".csv\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(bytes);
    }
}
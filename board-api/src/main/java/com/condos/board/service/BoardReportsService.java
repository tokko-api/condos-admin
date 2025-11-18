package com.condos.board.service;

import com.condos.board.api.dto.BoardReportRes;

import java.time.LocalDate;

public interface BoardReportsService {
    BoardReportRes buildReport(String orgId, LocalDate from, LocalDate to);
    String        buildCsv(String orgId, LocalDate from, LocalDate to);
    byte[]        buildXlsx(String orgId, LocalDate from, LocalDate to);
    byte[]        buildPdf(String orgId, LocalDate from, LocalDate to);
}
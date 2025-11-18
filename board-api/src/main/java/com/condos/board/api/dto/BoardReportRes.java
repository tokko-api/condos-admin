package com.condos.board.api.dto;

import java.util.List;

public record BoardReportRes(
        String orgId,
        String from,  // ISO date
        String to,    // ISO date
        long activeBoards,
        long openTasks,
        long overdueTasks,
        long completedLast30d,
        List<PerDay> tasksPerDay,
        List<PerBoard> tasksPerBoard,

        // 👇 NUEVOS CAMPOS
        List<LabelValue> statusPie,
        List<LabelValue> overdueByColony,
        List<LabelValue> topOverdueColonies
) {
    public record PerDay(String date, long count) {}
    public record PerBoard(String boardId, String boardName, long count) {}

    public record LabelValue(String label, long value) {}
}
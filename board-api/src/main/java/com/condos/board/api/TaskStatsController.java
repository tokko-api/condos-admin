package com.condos.board.api;

import com.condos.board.stats.TaskStatsService;
import com.condos.board.stats.dto.BoardRes;
import com.condos.board.stats.dto.CountRes;
import com.condos.board.stats.dto.DayRes;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/condos/api/board/tasks/stats")
public class TaskStatsController {

    private final TaskStatsService svc;

    public TaskStatsController(TaskStatsService svc) {
        this.svc = svc;
    }

    @GetMapping("/open-count")
    public CountRes open(@RequestParam String orgId) {
        return svc.openCount(orgId);
    }

    @GetMapping("/overdue-count")
    public CountRes overdue(@RequestParam String orgId) {
        return svc.overdueCount(orgId);
    }

    @GetMapping("/done-last-30d")
    public CountRes done30d(@RequestParam String orgId) {
        return svc.doneLast30d(orgId);
    }

    @GetMapping("/by-day")
    public List<DayRes> byDay(
            @RequestParam String orgId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return svc.byDay(orgId, from, to);
    }

    @GetMapping("/by-board")
    public List<BoardRes> byBoard(
            @RequestParam String orgId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return svc.byBoard(orgId, from, to);
    }
}
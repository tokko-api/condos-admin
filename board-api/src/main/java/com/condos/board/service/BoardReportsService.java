package com.condos.board.service;

import com.condos.board.api.dto.BoardReportCsv;
import com.condos.board.api.dto.BoardReportRes;
import com.condos.board.api.dto.BoardReportRes.PerBoard;
import com.condos.board.api.dto.BoardReportRes.PerDay;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
public class BoardReportsService {

    private final MongoTemplate mongo;

    public BoardReportsService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public BoardReportRes buildReport(String orgId, LocalDate from, LocalDate to) {
        // Fechas a Instants (rango [from, to + 1 day) en UTC)
        Instant start = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endExclusive = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant now = Instant.now();
        Instant last30d = Instant.now().minusSeconds(30L * 24 * 3600);

        long activeBoards = countActiveBoards(orgId);
        long openTasks = countOpenTasks(orgId);
        long overdueTasks = countOverdueTasks(orgId, now);
        long completed30 = countCompletedLast30d(orgId, last30d);

        List<PerDay> perDay = tasksPerDay(orgId, start, endExclusive);
        List<PerBoard> perBoard = tasksPerBoard(orgId, start, endExclusive);

        return new BoardReportRes(
                orgId,
                from.toString(),
                to.toString(),
                activeBoards,
                openTasks,
                overdueTasks,
                completed30,
                perDay,
                perBoard
        );
    }

    public BoardReportCsv buildCsv(String orgId, LocalDate from, LocalDate to) {
        BoardReportRes r = buildReport(orgId, from, to);

        StringBuilder sb = new StringBuilder();
        sb.append("orgId,from,to,activeBoards,openTasks,overdueTasks,completedLast30d\n");
        sb.append(String.join(",",
                        r.orgId(),
                        r.from(),
                        r.to(),
                        String.valueOf(r.activeBoards()),
                        String.valueOf(r.openTasks()),
                        String.valueOf(r.overdueTasks()),
                        String.valueOf(r.completedLast30d())))
                .append("\n\n");

        sb.append("TasksPerDay\n");
        sb.append("date,count\n");
        r.tasksPerDay().forEach(d -> sb.append(d.date()).append(",").append(d.count()).append("\n"));

        sb.append("\nTasksPerBoard\n");
        sb.append("boardId,boardName,count\n");
        r.tasksPerBoard().forEach(b ->
                sb.append(b.boardId()).append(",")
                        .append(escapeCsv(b.boardName())).append(",")
                        .append(b.count()).append("\n"));

        return new BoardReportCsv(sb.toString());
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    // --------- counts

    private long countActiveBoards(String orgId) {
        Criteria c = Criteria.where("orgId").is(orgId)
                .and("status").is("ACTIVE");
        return mongo.count(new org.springframework.data.mongodb.core.query.Query(c), "boards");
    }

    private long countOpenTasks(String orgId) {
        Criteria c = Criteria.where("orgId").is(orgId)
                .and("status").in(List.of("OPEN", "IN_PROGRESS"));
        return mongo.count(new org.springframework.data.mongodb.core.query.Query(c), "tasks");
    }

    private long countOverdueTasks(String orgId, Instant now) {
        Criteria c = Criteria.where("orgId").is(orgId)
                .and("dueDate").lt(Date.from(now))
                .and("status").nin(List.of("DONE", "CANCELLED", "ARCHIVED"));
        return mongo.count(new org.springframework.data.mongodb.core.query.Query(c), "tasks");
    }

    private long countCompletedLast30d(String orgId, Instant since) {
        Criteria c = Criteria.where("orgId").is(orgId)
                .and("status").is("DONE")
                .and("updatedAt").gte(Date.from(since));
        return mongo.count(new org.springframework.data.mongodb.core.query.Query(c), "tasks");
    }

    // --------- series

    /** Tareas creadas por día en el rango [start, end) */
    private List<PerDay> tasksPerDay(String orgId, Instant start, Instant endExclusive) {
        MatchOperation match = match(new Criteria().andOperator(
                Criteria.where("orgId").is(orgId),
                Criteria.where("createdAt").gte(Date.from(start)).lt(Date.from(endExclusive))
        ));
        // Mongo 5+: $dateTrunc. Si usas 4.2/4.4, usa $dateToString.
        AddFieldsOperation dayField = AddFieldsOperation.addField("day")
                .withValue(new Document("$dateToString",
                        new Document("format", "%Y-%m-%d").append("date", "$createdAt")))
                .build();

        GroupOperation group = group("day").count().as("count");
        SortOperation sort = sort(Sort.Direction.ASC, "_id");
        Aggregation agg = newAggregation(match, dayField, group, sort);

        List<Document> docs = mongo.aggregate(agg, "tasks", Document.class).getMappedResults();

        return docs.stream()
                .map(d -> new PerDay(d.getString("_id"), d.get("count", Number.class).longValue()))
                .collect(Collectors.toList());
    }

    /** Tareas por colonia (board) en el rango, con lookup de nombre */
    private List<PerBoard> tasksPerBoard(String orgId, Instant start, Instant endExclusive) {
        MatchOperation match = match(new Criteria().andOperator(
                Criteria.where("orgId").is(orgId),
                Criteria.where("createdAt").gte(Date.from(start)).lt(Date.from(endExclusive))
        ));
        GroupOperation group = group("boardId").count().as("count");
        Aggregation agg = newAggregation(match, group);

        List<Document> grouped = mongo.aggregate(agg, "tasks", Document.class).getMappedResults();
        if (grouped.isEmpty()) return List.of();

        // ids
        List<String> boardIds = grouped.stream().map(d -> String.valueOf(d.get("_id"))).toList();

        // nombres (un tiro a boards)
        Criteria cb = Criteria.where("orgId").is(orgId).and("id").in(boardIds);
        List<Document> boards = mongo.find(
                new org.springframework.data.mongodb.core.query.Query(cb),
                Document.class, "boards");

        Map<String, String> nameById = boards.stream().collect(Collectors.toMap(
                d -> String.valueOf(d.get("id")),
                d -> (String) Optional.ofNullable(d.get("name")).orElse(String.valueOf(d.get("id")))
        ));

        return grouped.stream()
                .map(d -> {
                    String id = String.valueOf(d.get("_id"));
                    long count = d.get("count", Number.class).longValue();
                    String name = nameById.getOrDefault(id, id);
                    return new PerBoard(id, name, count);
                })
                .sorted(Comparator.comparing(PerBoard::boardName))
                .toList();
    }
}
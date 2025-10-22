// com.condos.board.stats.TaskStatsService
package com.condos.board.stats;

import com.condos.board.stats.dto.BoardRes;
import com.condos.board.stats.dto.CountRes;
import com.condos.board.stats.dto.DayRes;
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
public class TaskStatsService {

    private final MongoTemplate mongo;

    public TaskStatsService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    // === contadores simples ===

    public CountRes openCount(String orgId) {
        var c = Criteria.where("orgId").is(orgId)
                .and("status").in(List.of("OPEN", "IN_PROGRESS"));
        long n = mongo.count(new org.springframework.data.mongodb.core.query.Query(c), "tasks");
        return new CountRes(n);
    }

    public CountRes overdueCount(String orgId) {
        var now = new Date();
        var c = Criteria.where("orgId").is(orgId)
                .and("dueDate").lt(now)
                .and("status").nin(List.of("DONE","CANCELLED","ARCHIVED"));
        long n = mongo.count(new org.springframework.data.mongodb.core.query.Query(c), "tasks");
        return new CountRes(n);
    }

    public CountRes doneLast30d(String orgId) {
        var since = Date.from(Instant.now().minusSeconds(30L*24*3600));
        var c = Criteria.where("orgId").is(orgId)
                .and("status").is("DONE")
                .and("updatedAt").gte(since);
        long n = mongo.count(new org.springframework.data.mongodb.core.query.Query(c), "tasks");
        return new CountRes(n);
    }

    // === series por día (en rango [from, to]) ===
    // open = creadas ese día con status OPEN/IN_PROGRESS
    // overdue = con dueDate ese día y no DONE/CANCELLED/ARCHIVED
    // done = cambiadas a DONE ese día (updatedAt)
    public List<DayRes> byDay(String orgId, LocalDate from, LocalDate to) {
        var start = Date.from(from.atStartOfDay().toInstant(ZoneOffset.UTC));
        var end   = Date.from(to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));

        // 1) abiertas por día (createdAt)
        var openDocs = groupByDay("createdAt",
                Criteria.where("orgId").is(orgId)
                        .and("createdAt").gte(start).lt(end)
                        .and("status").in(List.of("OPEN","IN_PROGRESS")));

        // 2) vencidas por día (dueDate)
        var overdueDocs = groupByDay("dueDate",
                Criteria.where("orgId").is(orgId)
                        .and("dueDate").gte(start).lt(end)
                        .and("status").nin(List.of("DONE","CANCELLED","ARCHIVED")));

        // 3) done por día (updatedAt)
        var doneDocs = groupByDay("updatedAt",
                Criteria.where("orgId").is(orgId)
                        .and("updatedAt").gte(start).lt(end)
                        .and("status").is("DONE"));

        Map<String, long[]> map = new TreeMap<>();
        for (var d : openDocs)   map.computeIfAbsent(d.getString("_id"), k -> new long[3])[0] = d.get("count", Number.class).longValue();
        for (var d : overdueDocs)map.computeIfAbsent(d.getString("_id"), k -> new long[3])[1] = d.get("count", Number.class).longValue();
        for (var d : doneDocs)   map.computeIfAbsent(d.getString("_id"), k -> new long[3])[2] = d.get("count", Number.class).longValue();

        return map.entrySet().stream()
                .map(e -> new DayRes(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                .collect(Collectors.toList());
    }

    // === serie por colonia (board) en rango [from, to], por status ===
    public List<BoardRes> byBoard(String orgId, LocalDate from, LocalDate to) {
        var start = Date.from(from.atStartOfDay().toInstant(ZoneOffset.UTC));
        var end   = Date.from(to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));

        // agrupar por boardId y status para tareas creadas en el rango
        MatchOperation match = match(new Criteria().andOperator(
                Criteria.where("orgId").is(orgId),
                Criteria.where("createdAt").gte(start).lt(end)
        ));
        GroupOperation group = group(fields().and("boardId").and("status")).count().as("count");
        Aggregation agg = newAggregation(match, group);
        var rows = mongo.aggregate(agg, "tasks", Document.class).getMappedResults();

        // sumar por board
        Map<String,long[]> acc = new HashMap<>(); // [open, inProgress, done]
        for (var d : rows) {
            var id = ((Document)d.get("_id")).get("boardId");
            var status = String.valueOf(((Document)d.get("_id")).get("status"));
            var arr = acc.computeIfAbsent(String.valueOf(id), k -> new long[3]);
            long c = d.get("count", Number.class).longValue();
            if ("OPEN".equals(status)) arr[0]+=c;
            else if ("IN_PROGRESS".equals(status)) arr[1]+=c;
            else if ("DONE".equals(status)) arr[2]+=c;
        }

        // nombres de board
        var boardIds = acc.keySet().stream().toList();
        Map<String,String> nameById = resolveBoardNames(orgId, boardIds);

        return acc.entrySet().stream()
                .map(e -> new BoardRes(e.getKey(), nameById.getOrDefault(e.getKey(), e.getKey()),
                        e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                .sorted(Comparator.comparing(BoardRes::boardName))
                .toList();
    }

    // ---- helpers ----

    private List<Document> groupByDay(String dateField, Criteria base) {
        MatchOperation match = match(base);
        AddFieldsOperation dayField = AddFieldsOperation.addField("day")
                .withValue(new Document("$dateToString",
                        new Document("format", "%Y-%m-%d").append("date", "$" + dateField)))
                .build();
        GroupOperation group = group("day").count().as("count");
        SortOperation sort = sort(Sort.Direction.ASC, "_id");
        var agg = newAggregation(match, dayField, group, sort);
        return mongo.aggregate(agg, "tasks", Document.class).getMappedResults();
    }

    private Map<String,String> resolveBoardNames(String orgId, List<String> boardIds) {
        if (boardIds.isEmpty()) return Map.of();
        var c = Criteria.where("orgId").is(orgId).and("id").in(boardIds);
        var docs = mongo.find(new org.springframework.data.mongodb.core.query.Query(c), Document.class, "boards");
        return docs.stream().collect(Collectors.toMap(
                d -> String.valueOf(d.get("id")),
                d -> String.valueOf(d.getOrDefault("name", d.get("id")))
        ));
    }
}
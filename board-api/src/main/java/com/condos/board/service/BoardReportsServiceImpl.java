package com.condos.board.service;

import com.condos.board.api.dto.BoardReportRes;
import com.condos.board.api.dto.BoardReportRes.PerBoard;
import com.condos.board.api.dto.BoardReportRes.PerDay;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
public class BoardReportsServiceImpl implements BoardReportsService {

    private final MongoTemplate mongo;

    public BoardReportsServiceImpl(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /* ========================== OVERVIEW ========================== */

    @Override
    public BoardReportRes buildReport(String orgId, LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endExclusive = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        Instant now = Instant.now();
        Instant last30d = Instant.now().minusSeconds(30L * 24 * 3600);

        long activeBoards  = countActiveBoards(orgId);
        long openTasks     = countOpenTasks(orgId);
        long overdueTasks  = countOverdueTasks(orgId, now);
        long completed30   = countCompletedLast30d(orgId, last30d);

        List<PerDay>   perDay   = tasksPerDay(orgId, start, endExclusive, from, to);
        List<PerBoard> perBoard = tasksPerBoard(orgId, start, endExclusive);

        // ---- statusPie (dona)
        var statusPie = List.of(
                new BoardReportRes.LabelValue("Abiertas", openTasks),
                new BoardReportRes.LabelValue("Vencidas", overdueTasks),
                new BoardReportRes.LabelValue("Completadas", completed30)
        );

        // ---- overdueByColony (porcentaje por board usando N del periodo)
        long total = perBoard.stream().mapToLong(PerBoard::count).sum();
        List<BoardReportRes.LabelValue> overdueByColony = perBoard.stream()
                .map(b -> new BoardReportRes.LabelValue(
                        b.boardName(),                  // 👈 nombre descriptivo
                        total > 0 ? Math.round(b.count() * 100.0 / total) : 0
                ))
                .toList();

        // ---- topOverdueColonies (top 5 descendente)
        List<BoardReportRes.LabelValue> topOverdueColonies = overdueByColony.stream()
                .sorted((a, b) -> Long.compare(b.value(), a.value()))
                .limit(5)
                .toList();

        return new BoardReportRes(
                orgId,
                from.toString(),
                to.toString(),
                activeBoards,
                openTasks,
                overdueTasks,
                completed30,
                perDay,
                perBoard,
                statusPie,
                overdueByColony,
                topOverdueColonies
        );
    }

    /* ========================== CSV ========================== */

    @Override
    public String buildCsv(String orgId, LocalDate from, LocalDate to) {
        BoardReportRes r = buildReport(orgId, from, to);

        StringBuilder sb = new StringBuilder();
        // KPIs
        sb.append("orgId,from,to,activeBoards,openTasks,overdueTasks,completedLast30d\n");
        sb.append(String.join(",",
                esc(r.orgId()),
                esc(r.from()),
                esc(r.to()),
                String.valueOf(r.activeBoards()),
                String.valueOf(r.openTasks()),
                String.valueOf(r.overdueTasks()),
                String.valueOf(r.completedLast30d())
        )).append("\n\n");

        // Serie por día
        sb.append("TasksPerDay\n");
        sb.append("date,count\n");
        r.tasksPerDay().forEach(d -> sb.append(esc(d.date())).append(",").append(d.count()).append("\n"));

        // Por board
        sb.append("\nTasksPerBoard\n");
        sb.append("boardId,boardName,count\n");
        r.tasksPerBoard().forEach(b -> sb
                .append(esc(b.boardId())).append(",")
                .append(esc(b.boardName())).append(",")
                .append(b.count()).append("\n"));

        return sb.toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    /* ========================== XLSX ========================== */

    @Override
    public byte[] buildXlsx(String orgId, LocalDate from, LocalDate to) {
        var r = buildReport(orgId, from, to);

        try (var wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             var bos = new ByteArrayOutputStream()) {

            var headerStyle = wb.createCellStyle();
            var headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            // Hoja KPIs
            var s1 = wb.createSheet("KPIs");
            int rIdx = 0;
            var title = s1.createRow(rIdx++); title.createCell(0).setCellValue("Condos Admin — Reporte");
            var headers = s1.createRow(rIdx++);
            String[] k = {"orgId","from","to","activeBoards","openTasks","overdueTasks","completedLast30d"};
            for (int i=0;i<k.length;i++) { var c=headers.createCell(i); c.setCellValue(k[i]); c.setCellStyle(headerStyle); }
            var row = s1.createRow(rIdx++);
            row.createCell(0).setCellValue(r.orgId());
            row.createCell(1).setCellValue(r.from());
            row.createCell(2).setCellValue(r.to());
            row.createCell(3).setCellValue(r.activeBoards());
            row.createCell(4).setCellValue(r.openTasks());
            row.createCell(5).setCellValue(r.overdueTasks());
            row.createCell(6).setCellValue(r.completedLast30d());
            for (int i=0;i<k.length;i++) s1.autoSizeColumn(i);

            // Hoja Per Day
            var s2 = wb.createSheet("TasksPerDay");
            rIdx = 0;
            var h2 = s2.createRow(rIdx++);
            String[] h2c = {"date","count"};
            for (int i=0;i<h2c.length;i++){ var c=h2.createCell(i); c.setCellValue(h2c[i]); c.setCellStyle(headerStyle); }
            for (PerDay d : r.tasksPerDay()) {
                var rr = s2.createRow(rIdx++);
                rr.createCell(0).setCellValue(d.date());
                rr.createCell(1).setCellValue(d.count());
            }
            s2.autoSizeColumn(0); s2.autoSizeColumn(1);

            // Hoja Per Board
            var s3 = wb.createSheet("TasksPerBoard");
            rIdx = 0;
            var h3 = s3.createRow(rIdx++);
            String[] h3c = {"boardId","boardName","count"};
            for (int i=0;i<h3c.length;i++){ var c=h3.createCell(i); c.setCellValue(h3c[i]); c.setCellStyle(headerStyle); }
            for (PerBoard b : r.tasksPerBoard()) {
                var rr = s3.createRow(rIdx++);
                rr.createCell(0).setCellValue(b.boardId());
                rr.createCell(1).setCellValue(b.boardName());
                rr.createCell(2).setCellValue(b.count());
            }
            for (int i=0;i<h3c.length;i++) s3.autoSizeColumn(i);

            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build XLSX", e);
        }
    }

    /* ========================== PDF ========================== */

    @Override
    public byte[] buildPdf(String orgId, LocalDate from, LocalDate to) {
        var r = buildReport(orgId, from, to);

        try (var bos = new ByteArrayOutputStream()) {
            var doc = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4.rotate(), 24,24,24,24);
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, bos);
            doc.open();

            var titleFont  = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 16);
            var headerFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 10);
            var cellFont   = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 9);

            doc.add(new com.lowagie.text.Paragraph("Condos Admin — Reporte (" + from + " → " + to + ")", titleFont));
            doc.add(new com.lowagie.text.Paragraph("Org: " + orgId));
            doc.add(new com.lowagie.text.Paragraph(" "));

            // KPIs
            table(doc, headerFont, cellFont,
                    new String[]{"Métrica","Valor"},
                    List.of(
                            new String[]{"Colonias activas", String.valueOf(r.activeBoards())},
                            new String[]{"Tareas abiertas", String.valueOf(r.openTasks())},
                            new String[]{"Vencidas", String.valueOf(r.overdueTasks())},
                            new String[]{"Completadas (30d)", String.valueOf(r.completedLast30d())}
                    ));

            doc.add(new com.lowagie.text.Paragraph(" "));

            // PerDay
            table(doc, headerFont, cellFont,
                    new String[]{"Fecha","Conteo"},
                    r.tasksPerDay().stream()
                            .map(d -> new String[]{d.date(), String.valueOf(d.count())})
                            .toList());

            doc.add(new com.lowagie.text.Paragraph(" "));

            // PerBoard
            table(doc, headerFont, cellFont,
                    new String[]{"BoardId","BoardName","Conteo"},
                    r.tasksPerBoard().stream()
                            .map(b -> new String[]{b.boardId(), b.boardName(), String.valueOf(b.count())})
                            .toList());

            doc.close();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build PDF", e);
        }
    }

    private static void table(com.lowagie.text.Document doc,
                              com.lowagie.text.Font headerFont,
                              com.lowagie.text.Font cellFont,
                              String[] headers,
                              List<String[]> rows) throws com.lowagie.text.DocumentException {
        var table = new com.lowagie.text.pdf.PdfPTable(headers.length);
        table.setWidthPercentage(100);
        for (String h : headers) {
            var hc = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(h, headerFont));
            hc.setGrayFill(0.90f);
            table.addCell(hc);
        }
        for (String[] r : rows) for (String c : r) table.addCell(new com.lowagie.text.Phrase(c, cellFont));
        doc.add(table);
    }

    /* ========================== Aggregations ========================== */

    private long countActiveBoards(String orgId) {
        var c = Criteria.where("orgId").is(orgId).and("status").is("ACTIVE");
        return mongo.count(new org.springframework.data.mongodb.core.query.Query(c), "boards");
    }

    private long countOpenTasks(String orgId) {
        var c = Criteria.where("orgId").is(orgId).and("status").in(List.of("OPEN","IN_PROGRESS"));
        return mongo.count(new org.springframework.data.mongodb.core.query.Query(c), "tasks");
    }

    private long countOverdueTasks(String orgId, Instant now) {
        var c = Criteria.where("orgId").is(orgId)
                .and("dueDate").lt(Date.from(now))
                .and("status").nin(List.of("DONE","CANCELLED","ARCHIVED"));
        return mongo.count(new org.springframework.data.mongodb.core.query.Query(c), "tasks");
    }

    private long countCompletedLast30d(String orgId, Instant since) {
        var c = Criteria.where("orgId").is(orgId)
                .and("status").is("DONE")
                .and("updatedAt").gte(Date.from(since));
        return mongo.count(new org.springframework.data.mongodb.core.query.Query(c), "tasks");
    }

    /* ======= Serie por día usando “fecha de actividad” y rellenando días ======= */

    private static Document activityDateExpr() {
        // updatedAt si existe; de lo contrario createdAt
        return new Document("$ifNull", List.of("$updatedAt", "$createdAt"));
    }

    private static List<String> dayBuckets(LocalDate from, LocalDate to) {
        List<String> out = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) out.add(d.toString());
        return out;
    }

    private List<PerDay> tasksPerDay(String orgId, Instant start, Instant endExclusive, LocalDate from, LocalDate to) {
        // actividad dentro del rango (updatedAt en rango o, si no existe, createdAt en rango)
        MatchOperation match = match(new Criteria().andOperator(
                Criteria.where("orgId").is(orgId),
                new Criteria().orOperator(
                        Criteria.where("updatedAt").gte(Date.from(start)).lt(Date.from(endExclusive)),
                        new Criteria().andOperator(
                                Criteria.where("updatedAt").exists(false),
                                Criteria.where("createdAt").gte(Date.from(start)).lt(Date.from(endExclusive))
                        )
                )
        ));

        AddFieldsOperation dayField = AddFieldsOperation.addField("day")
                .withValue(new Document("$dateToString",
                        new Document("format", "%Y-%m-%d").append("date", activityDateExpr())))
                .build();

        GroupOperation group = group("day").count().as("count");
        SortOperation sort = sort(Sort.Direction.ASC, "_id");
        Aggregation agg = newAggregation(match, dayField, group, sort);

        List<Document> docs = mongo.aggregate(agg, "tasks", Document.class).getMappedResults();
        Map<String, Long> byDay = docs.stream()
                .collect(Collectors.toMap(d -> d.getString("_id"),
                        d -> d.get("count", Number.class).longValue()));

        // Rellenar todos los días del rango con 0
        List<PerDay> out = new ArrayList<>();
        for (String day : dayBuckets(from, to)) {
            out.add(new PerDay(day, byDay.getOrDefault(day, 0L)));
        }
        return out;
    }

    /* ======= Serie por board: tareas activas en el periodo ======= */

    private List<PerBoard> tasksPerBoard(String orgId, Instant start, Instant endExclusive) {
        Criteria activeInWindow = new Criteria().andOperator(
                Criteria.where("orgId").is(orgId),
                Criteria.where("createdAt").lt(Date.from(endExclusive)),
                new Criteria().orOperator(
                        Criteria.where("updatedAt").gte(Date.from(start)).lt(Date.from(endExclusive)),
                        Criteria.where("status").in(List.of("OPEN","IN_PROGRESS"))
                )
        );

        MatchOperation match = match(activeInWindow);
        GroupOperation group = group("boardId").count().as("count");
        Aggregation agg = newAggregation(match, group);

        List<Document> grouped = mongo.aggregate(agg, "tasks", Document.class).getMappedResults();
        if (grouped.isEmpty()) return List.of();

        // ids tal como vienen del group (pueden ser ObjectId o String)
        List<String> boardIds = grouped.stream()
                .map(d -> String.valueOf(d.get("_id")))
                .toList();

        // Convierte a ObjectId donde aplique
        List<org.bson.types.ObjectId> objIds = boardIds.stream()
                .filter(s -> s != null && s.matches("^[0-9a-fA-F]{24}$"))
                .map(org.bson.types.ObjectId::new)
                .toList();

        // Busca por _id (ObjectId) O por id (String), y siempre mismo orgId
        Criteria cb = new Criteria().andOperator(
                Criteria.where("orgId").is(orgId),
                new Criteria().orOperator(
                        Criteria.where("_id").in(objIds),   // boards con _id:ObjectId
                        Criteria.where("id").in(boardIds)   // boards con id:String
                )
        );

        List<Document> boards = mongo.find(new org.springframework.data.mongodb.core.query.Query(cb),
                Document.class, "boards");

        // Construye mapa id->name resolviendo _id o id
        Map<String, String> nameById = boards.stream().collect(Collectors.toMap(
                d -> {
                    Object raw = d.get("id");
                    if (raw == null) raw = d.get("_id"); // puede ser ObjectId
                    return String.valueOf(raw);
                },
                d -> {
                    Object nm = d.get("name");
                    if (nm == null) nm = d.get("title"); // por si acaso
                    Object rawId = d.get("id") != null ? d.get("id") : d.get("_id");
                    return String.valueOf(nm != null ? nm : rawId);
                }
        ));

        // Mapea cada grupo usando el nombre (si no existe, deja el id)
        return grouped.stream()
                .map(d -> {
                    String id = String.valueOf(d.get("_id")); // puede ser hex de ObjectId
                    long count = d.get("count", Number.class).longValue();
                    String name = nameById.getOrDefault(id, id);
                    return new PerBoard(id, name, count);
                })
                .sorted(Comparator.comparing(PerBoard::boardName))
                .toList();
    }
}
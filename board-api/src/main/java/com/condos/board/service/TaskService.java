// src/main/java/com/condos/board/service/TaskService.java
package com.condos.board.service;

import com.condos.board.model.Task;
import com.condos.board.model.TaskStatus;
import com.condos.board.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository repo;

    public Task create(String orgId, String boardId, String title, String description,
                       String assigneeId, String dueDate) {
        Task t = Task.newTask(orgId, boardId, title, description, assigneeId, parseDueDate(dueDate));
        return repo.save(t);
    }

    public Optional<Task> get(String id) {
        return repo.findById(id);
    }

    public Task update(String id, String title, String description, String assigneeId, String dueDate) {
        Task t = repo.findById(id).orElseThrow(NoSuchElementException::new);
        t.setTitle(title);
        t.setDescription(description);
        t.setAssigneeId(assigneeId);
        t.setDueDate(parseDueDate(dueDate));
        t.setUpdatedAt(Instant.now());
        return repo.save(t);
    }

    private Instant parseDueDate(String s) {
        if (!StringUtils.hasText(s)) return null;
        return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE)
                .atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public Page<Task> list(String boardId, String q, int page, int size, String sortBy, Sort.Direction dir) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));
        if (q == null || q.isBlank()) {
            return repo.findByBoardId(boardId, pageable);
        }
        String regex = ".*" + q + ".*";
        return repo.findByBoardIdAndTitleRegexIgnoreCase(boardId, regex, pageable);
    }

    public Page<Task> listActive(String boardId, String q, int page, int size, String sortBy, Sort.Direction dir) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));
        if (q == null || q.isBlank()) {
            return repo.findByBoardIdAndStatusNot(boardId, TaskStatus.ARCHIVED, pageable);
        }
        String regex = ".*" + q + ".*";
        return repo.findByBoardIdAndStatusNotAndTitleRegexIgnoreCase(boardId, TaskStatus.ARCHIVED, regex, pageable);
    }

    public Task changeStatus(String id, TaskStatus status) {
        Task t = repo.findById(id).orElseThrow(NoSuchElementException::new);
        t.setStatus(status);
        t.setUpdatedAt(Instant.now());
        return repo.save(t);
    }

    /** Soft delete = ARCHIVED */
    public void delete(String id) {
        Task t = repo.findById(id).orElseThrow(NoSuchElementException::new);
        t.setStatus(TaskStatus.ARCHIVED);
        t.setUpdatedAt(Instant.now());
        repo.save(t);
    }
    public Page<Task> listByAssignee(String orgId, String assigneeId,
                                     TaskStatus status, boolean overdue, String q,
                                     int page, int size, String sortBy, Sort.Direction dir) {
        var pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));
        Page<Task> p;

        if (status != null) {
            p = repo.findByOrgIdAndAssigneeIdAndStatus(orgId, assigneeId, status, pageable);
        } else {
            p = repo.findByOrgIdAndAssigneeId(orgId, assigneeId, pageable);
        }

        // filtros simples en memoria (si no quieres query compleja)
        var now = Instant.now();
        var filtered = p.getContent().stream()
                .filter(t -> !overdue || (t.getDueDate() != null && t.getDueDate().isBefore(now)
                        && t.getStatus() != TaskStatus.DONE && t.getStatus() != TaskStatus.CANCELED))
                .filter(t -> q == null || q.isBlank()
                        || (t.getTitle()!=null && t.getTitle().toLowerCase().contains(q.toLowerCase()))
                        || (t.getDescription()!=null && t.getDescription().toLowerCase().contains(q.toLowerCase())))
                .toList();

        return new PageImpl<>(filtered, pageable, filtered.size());
    }

}
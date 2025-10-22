package com.condos.board.repository;

import com.condos.board.model.Task;
import com.condos.board.model.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TaskRepository extends MongoRepository<Task, String> {

    Page<Task> findByBoardId(String boardId, Pageable pageable);

    Page<Task> findByBoardIdAndStatusNot(String boardId, TaskStatus status, Pageable pageable);

    Page<Task> findByBoardIdAndTitleRegexIgnoreCase(String boardId, String q, Pageable pageable);

    Page<Task> findByBoardIdAndStatusNotAndTitleRegexIgnoreCase(
            String boardId, TaskStatus status, String q, Pageable pageable);

    Page<Task> findByOrgIdAndAssigneeId(String orgId, String assigneeId, Pageable pageable);
    Page<Task> findByOrgIdAndAssigneeIdAndStatus(String orgId, String assigneeId,
                                                 TaskStatus status, Pageable pageable);
}
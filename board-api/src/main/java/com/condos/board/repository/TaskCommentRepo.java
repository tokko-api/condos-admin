package com.condos.board.repository;

import com.condos.board.api.model.TaskCommentDoc;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;


public interface TaskCommentRepo extends ReactiveMongoRepository<TaskCommentDoc, String> {
    Flux<TaskCommentDoc> findByTaskIdOrderByCreatedAtAsc(String taskId, PageRequest pageable);
}
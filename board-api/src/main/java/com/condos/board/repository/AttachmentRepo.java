package com.condos.board.repository;

import com.condos.board.model.AttachmentDoc;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface AttachmentRepo extends ReactiveMongoRepository<AttachmentDoc, String> {
    Flux<AttachmentDoc> findByTaskId(String taskId, PageRequest pageable);
}


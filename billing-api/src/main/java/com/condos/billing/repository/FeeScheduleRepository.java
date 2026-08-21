package com.condos.billing.repository;

import com.condos.billing.model.FeeSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FeeScheduleRepository extends MongoRepository<FeeSchedule, String> {
    Page<FeeSchedule> findByBoardId(String boardId, Pageable pageable);
    List<FeeSchedule> findByBoardIdAndActiveTrue(String boardId);
}

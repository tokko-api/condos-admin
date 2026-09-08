package com.condos.billing.repository;

import com.condos.billing.model.Expense;
import com.condos.billing.model.ExpenseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ExpenseRepository extends MongoRepository<Expense, String> {

    Page<Expense> findByBoardId(String boardId, Pageable pageable);

    Page<Expense> findByBoardIdAndStatus(String boardId, ExpenseStatus status, Pageable pageable);

    Page<Expense> findByBoardIdAndPeriod(String boardId, String period, Pageable pageable);

    Page<Expense> findByBoardIdAndPeriodAndStatus(String boardId, String period, ExpenseStatus status, Pageable pageable);

    List<Expense> findByOrgIdAndPeriod(String orgId, String period);
}

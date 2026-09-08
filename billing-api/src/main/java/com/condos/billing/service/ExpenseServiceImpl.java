package com.condos.billing.service;

import com.condos.billing.model.Expense;
import com.condos.billing.model.ExpenseCategory;
import com.condos.billing.model.ExpenseStatus;
import com.condos.billing.model.PaymentMethod;
import com.condos.billing.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenses;

    @Override
    public Expense create(String orgId, String boardId, String concept, ExpenseCategory category, String providerName,
                           BigDecimal amount, PaymentMethod method, LocalDate expenseDate,
                           String receiptFileId, String receiptFileName, String notes, String registeredBy) {
        Expense e = Expense.newExpense(orgId, boardId, concept, category, providerName, amount, method,
                expenseDate, receiptFileId, receiptFileName, notes, registeredBy);
        return expenses.save(e);
    }

    @Override
    public Optional<Expense> get(String id) {
        return expenses.findById(id);
    }

    @Override
    public Page<Expense> listByBoard(String boardId, String period, boolean includeCancelled,
                                      int page, int size, String sortBy, Sort.Direction dir) {
        var pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));
        if (period != null && !period.isBlank()) {
            return includeCancelled
                    ? expenses.findByBoardIdAndPeriod(boardId, period, pageable)
                    : expenses.findByBoardIdAndPeriodAndStatus(boardId, period, ExpenseStatus.ACTIVE, pageable);
        }
        return includeCancelled
                ? expenses.findByBoardId(boardId, pageable)
                : expenses.findByBoardIdAndStatus(boardId, ExpenseStatus.ACTIVE, pageable);
    }

    @Override
    public Expense cancel(String id) {
        Expense e = expenses.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "expense not found"));
        e.setStatus(ExpenseStatus.CANCELLED);
        e.setUpdatedAt(java.time.Instant.now());
        return expenses.save(e);
    }
}

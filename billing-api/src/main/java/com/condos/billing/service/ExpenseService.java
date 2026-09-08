package com.condos.billing.service;

import com.condos.billing.model.Expense;
import com.condos.billing.model.ExpenseCategory;
import com.condos.billing.model.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface ExpenseService {

    Expense create(String orgId, String boardId, String concept, ExpenseCategory category, String providerName,
                    BigDecimal amount, PaymentMethod method, LocalDate expenseDate,
                    String receiptFileId, String receiptFileName, String notes, String registeredBy);

    Optional<Expense> get(String id);

    Page<Expense> listByBoard(String boardId, String period, boolean includeCancelled,
                               int page, int size, String sortBy, Sort.Direction dir);

    Expense cancel(String id);
}

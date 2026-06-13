package com.gmeo.finance_tracker.dashboard;

import com.gmeo.finance_tracker.common.exception.BadRequestException;
import com.gmeo.finance_tracker.dashboard.dto.DashboardSummaryResponse;
import com.gmeo.finance_tracker.security.CurrentUserService;
import com.gmeo.finance_tracker.transaction.Transaction;
import com.gmeo.finance_tracker.transaction.TransactionRepository;
import com.gmeo.finance_tracker.transaction.TransactionSpecification;
import com.gmeo.finance_tracker.transaction.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;

    public DashboardService(
            TransactionRepository transactionRepository,
            CurrentUserService currentUserService) {
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
    }

    public DashboardSummaryResponse getSummary(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate must be on or before toDate");
        }

        Long currentUserId = currentUserService.getCurrentUser().getId();
        Specification<Transaction> specification = TransactionSpecification.withFilters(
                currentUserId,
                null,
                null,
                fromDate,
                toDate,
                null,
                null);
        List<Transaction> transactions = transactionRepository.findAll(specification);

        BigDecimal totalIncome = sumByType(transactions, TransactionType.INCOME);
        BigDecimal totalExpense = sumByType(transactions, TransactionType.EXPENSE);

        return new DashboardSummaryResponse(
                totalIncome,
                totalExpense,
                totalIncome.subtract(totalExpense),
                transactions.size());
    }

    private BigDecimal sumByType(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

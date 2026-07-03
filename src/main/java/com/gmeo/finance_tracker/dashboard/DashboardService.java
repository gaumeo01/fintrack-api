package com.gmeo.finance_tracker.dashboard;

import com.gmeo.finance_tracker.common.exception.BadRequestException;
import com.gmeo.finance_tracker.dashboard.dto.DashboardCategoryBreakdownItem;
import com.gmeo.finance_tracker.dashboard.dto.DashboardCategoryBreakdownResponse;
import com.gmeo.finance_tracker.dashboard.dto.DashboardSummaryResponse;
import com.gmeo.finance_tracker.dashboard.dto.DashboardTrendItem;
import com.gmeo.finance_tracker.dashboard.dto.DashboardTrendResponse;
import com.gmeo.finance_tracker.dashboard.enums.DashboardTrendGroupBy;
import com.gmeo.finance_tracker.security.CurrentUserService;
import com.gmeo.finance_tracker.transaction.Transaction;
import com.gmeo.finance_tracker.transaction.TransactionRepository;
import com.gmeo.finance_tracker.transaction.TransactionSpecification;
import com.gmeo.finance_tracker.transaction.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        validateDateRange(fromDate, toDate);

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

    public DashboardCategoryBreakdownResponse getCategoryBreakdown(
            LocalDate fromDate,
            LocalDate toDate,
            TransactionType type) {
        validateDateRange(fromDate, toDate);

        Long currentUserId = currentUserService.getCurrentUser().getId();
        Specification<Transaction> specification = TransactionSpecification.withFilters(
                currentUserId,
                type,
                null,
                fromDate,
                toDate,
                null,
                null);
        List<Transaction> transactions = transactionRepository.findAll(specification);

        Map<Long, CategoryTotals> totalsByCategory = new HashMap<>();
        for (Transaction transaction : transactions) {
            totalsByCategory
                    .computeIfAbsent(
                            transaction.getCategory().getId(),
                            categoryId -> new CategoryTotals(transaction.getCategory().getName()))
                    .add(transaction.getAmount());
        }

        List<DashboardCategoryBreakdownItem> items = totalsByCategory.entrySet().stream()
                .map(entry -> new DashboardCategoryBreakdownItem(
                        entry.getKey(),
                        entry.getValue().categoryName,
                        type,
                        entry.getValue().totalAmount,
                        entry.getValue().transactionCount))
                .sorted(Comparator.comparing(DashboardCategoryBreakdownItem::getTotalAmount).reversed())
                .toList();

        BigDecimal totalAmount = items.stream()
                .map(DashboardCategoryBreakdownItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardCategoryBreakdownResponse(
                fromDate,
                toDate,
                type,
                totalAmount,
                transactions.size(),
                items);
    }

    public DashboardTrendResponse getTrend(
            LocalDate fromDate,
            LocalDate toDate,
            DashboardTrendGroupBy groupBy) {
        validateDateRange(fromDate, toDate);

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

        Map<String, TrendTotals> totalsByPeriod = createEmptyTrendPeriods(fromDate, toDate, groupBy);
        for (Transaction transaction : transactions) {
            totalsByPeriod.get(toPeriod(transaction.getTransactionDate(), groupBy))
                    .add(transaction.getType(), transaction.getAmount());
        }

        List<DashboardTrendItem> items = totalsByPeriod.entrySet().stream()
                .map(entry -> new DashboardTrendItem(
                        entry.getKey(),
                        entry.getValue().incomeAmount,
                        entry.getValue().expenseAmount,
                        entry.getValue().incomeAmount.subtract(entry.getValue().expenseAmount),
                        entry.getValue().transactionCount))
                .toList();

        return new DashboardTrendResponse(fromDate, toDate, groupBy, items);
    }

    private BigDecimal sumByType(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate must be on or before toDate");
        }
    }

    private Map<String, TrendTotals> createEmptyTrendPeriods(
            LocalDate fromDate,
            LocalDate toDate,
            DashboardTrendGroupBy groupBy) {
        Map<String, TrendTotals> totalsByPeriod = new LinkedHashMap<>();

        if (groupBy == DashboardTrendGroupBy.DAY) {
            for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
                totalsByPeriod.put(date.toString(), new TrendTotals());
            }
            return totalsByPeriod;
        }

        YearMonth endMonth = YearMonth.from(toDate);
        for (YearMonth month = YearMonth.from(fromDate); !month.isAfter(endMonth); month = month.plusMonths(1)) {
            totalsByPeriod.put(month.toString(), new TrendTotals());
        }
        return totalsByPeriod;
    }

    private String toPeriod(LocalDate transactionDate, DashboardTrendGroupBy groupBy) {
        if (groupBy == DashboardTrendGroupBy.DAY) {
            return transactionDate.toString();
        }
        return YearMonth.from(transactionDate).toString();
    }

    private static class CategoryTotals {

        private final String categoryName;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private long transactionCount;

        private CategoryTotals(String categoryName) {
            this.categoryName = categoryName;
        }

        private void add(BigDecimal amount) {
            totalAmount = totalAmount.add(amount);
            transactionCount++;
        }
    }

    private static class TrendTotals {

        private BigDecimal incomeAmount = BigDecimal.ZERO;
        private BigDecimal expenseAmount = BigDecimal.ZERO;
        private long transactionCount;

        private void add(TransactionType type, BigDecimal amount) {
            if (type == TransactionType.INCOME) {
                incomeAmount = incomeAmount.add(amount);
            } else {
                expenseAmount = expenseAmount.add(amount);
            }
            transactionCount++;
        }
    }
}

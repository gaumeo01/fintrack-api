package com.gmeo.finance_tracker.report;

import com.gmeo.finance_tracker.common.util.DateTimeUtils;
import com.gmeo.finance_tracker.report.dto.MonthlyReportCategoryItem;
import com.gmeo.finance_tracker.report.dto.MonthlyReportDailyTrendItem;
import com.gmeo.finance_tracker.report.dto.MonthlyReportResponse;
import com.gmeo.finance_tracker.security.CurrentUserService;
import com.gmeo.finance_tracker.transaction.Transaction;
import com.gmeo.finance_tracker.transaction.TransactionRepository;
import com.gmeo.finance_tracker.transaction.TransactionSpecification;
import com.gmeo.finance_tracker.transaction.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;

    public ReportService(TransactionRepository transactionRepository, CurrentUserService currentUserService) {
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
    }

    public MonthlyReportResponse getMonthlyReport(String month) {
        LocalDate fromDate = DateTimeUtils.parseMonthStart(month);
        LocalDate toDate = fromDate.withDayOfMonth(fromDate.lengthOfMonth());
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

        return new MonthlyReportResponse(
                DateTimeUtils.formatMonth(fromDate),
                fromDate,
                toDate,
                totalIncome,
                totalExpense,
                totalIncome.subtract(totalExpense),
                transactions.size(),
                categoryItems(transactions, TransactionType.EXPENSE),
                categoryItems(transactions, TransactionType.INCOME),
                dailyTrendItems(fromDate, toDate, transactions));
    }

    private BigDecimal sumByType(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<MonthlyReportCategoryItem> categoryItems(List<Transaction> transactions, TransactionType type) {
        Map<Long, CategoryTotals> totalsByCategory = new HashMap<>();
        transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .forEach(transaction -> totalsByCategory
                        .computeIfAbsent(
                                transaction.getCategory().getId(),
                                categoryId -> new CategoryTotals(transaction.getCategory().getName()))
                        .add(transaction.getAmount()));

        return totalsByCategory.entrySet().stream()
                .map(entry -> new MonthlyReportCategoryItem(
                        entry.getKey(),
                        entry.getValue().categoryName,
                        entry.getValue().amount,
                        entry.getValue().transactionCount))
                .sorted(Comparator.comparing(MonthlyReportCategoryItem::getAmount).reversed())
                .toList();
    }

    private List<MonthlyReportDailyTrendItem> dailyTrendItems(
            LocalDate fromDate,
            LocalDate toDate,
            List<Transaction> transactions) {
        Map<LocalDate, DailyTotals> totalsByDate = new LinkedHashMap<>();
        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            totalsByDate.put(date, new DailyTotals());
        }

        for (Transaction transaction : transactions) {
            totalsByDate.get(transaction.getTransactionDate())
                    .add(transaction.getType(), transaction.getAmount());
        }

        return totalsByDate.entrySet().stream()
                .map(entry -> new MonthlyReportDailyTrendItem(
                        entry.getKey(),
                        entry.getValue().income,
                        entry.getValue().expense,
                        entry.getValue().income.subtract(entry.getValue().expense),
                        entry.getValue().transactionCount))
                .toList();
    }

    private static class CategoryTotals {

        private final String categoryName;
        private BigDecimal amount = BigDecimal.ZERO;
        private long transactionCount;

        private CategoryTotals(String categoryName) {
            this.categoryName = categoryName;
        }

        private void add(BigDecimal amount) {
            this.amount = this.amount.add(amount);
            transactionCount++;
        }
    }

    private static class DailyTotals {

        private BigDecimal income = BigDecimal.ZERO;
        private BigDecimal expense = BigDecimal.ZERO;
        private long transactionCount;

        private void add(TransactionType type, BigDecimal amount) {
            if (type == TransactionType.INCOME) {
                income = income.add(amount);
            } else {
                expense = expense.add(amount);
            }
            transactionCount++;
        }
    }
}

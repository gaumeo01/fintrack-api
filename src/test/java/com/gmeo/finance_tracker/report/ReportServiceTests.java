package com.gmeo.finance_tracker.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.gmeo.finance_tracker.category.Category;
import com.gmeo.finance_tracker.category.enums.CategoryType;
import com.gmeo.finance_tracker.report.dto.MonthlyReportResponse;
import com.gmeo.finance_tracker.security.CurrentUserService;
import com.gmeo.finance_tracker.transaction.Transaction;
import com.gmeo.finance_tracker.transaction.TransactionRepository;
import com.gmeo.finance_tracker.transaction.enums.TransactionType;
import com.gmeo.finance_tracker.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.jpa.domain.Specification;

class ReportServiceTests {

    private TransactionRepository transactionRepository;
    private CurrentUserService currentUserService;
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        transactionRepository = Mockito.mock(TransactionRepository.class);
        currentUserService = Mockito.mock(CurrentUserService.class);
        reportService = new ReportService(transactionRepository, currentUserService);

        User user = new User();
        user.setId(7L);
        when(currentUserService.getCurrentUser()).thenReturn(user);
    }

    @Test
    void monthlyReportIncludesTotalsBalanceAndTransactionCount() {
        when(transactionRepository.findAll(Mockito.<Specification<Transaction>>any()))
                .thenReturn(List.of(
                        transaction(1L, TransactionType.INCOME, "1000.00", "Salary", LocalDate.of(2026, 6, 1)),
                        transaction(2L, TransactionType.EXPENSE, "400.00", "Food", LocalDate.of(2026, 6, 2))));

        MonthlyReportResponse response = reportService.getMonthlyReport("2026-06");

        assertThat(response.getMonth()).isEqualTo("2026-06");
        assertThat(response.getFromDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(response.getToDate()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(response.getTotalIncome()).isEqualByComparingTo("1000.00");
        assertThat(response.getTotalExpense()).isEqualByComparingTo("400.00");
        assertThat(response.getBalance()).isEqualByComparingTo("600.00");
        assertThat(response.getTransactionCount()).isEqualTo(2);
    }

    @Test
    void monthlyReportSortsTopCategoriesByAmountDescending() {
        when(transactionRepository.findAll(Mockito.<Specification<Transaction>>any()))
                .thenReturn(List.of(
                        transaction(1L, TransactionType.EXPENSE, "20.00", "Coffee", LocalDate.of(2026, 6, 1)),
                        transaction(2L, TransactionType.EXPENSE, "300.00", "Food", LocalDate.of(2026, 6, 2)),
                        transaction(3L, TransactionType.INCOME, "500.00", "Bonus", LocalDate.of(2026, 6, 3)),
                        transaction(4L, TransactionType.INCOME, "1000.00", "Salary", LocalDate.of(2026, 6, 4))));

        MonthlyReportResponse response = reportService.getMonthlyReport("2026-06");

        assertThat(response.getTopExpenseCategories())
                .extracting(item -> item.getCategoryName())
                .containsExactly("Food", "Coffee");
        assertThat(response.getTopIncomeCategories())
                .extracting(item -> item.getCategoryName())
                .containsExactly("Salary", "Bonus");
    }

    @Test
    void monthlyReportDailyTrendIncludesZeroValueDaysInChronologicalOrder() {
        when(transactionRepository.findAll(Mockito.<Specification<Transaction>>any()))
                .thenReturn(List.of(
                        transaction(1L, TransactionType.INCOME, "1000.00", "Salary", LocalDate.of(2026, 6, 2)),
                        transaction(2L, TransactionType.EXPENSE, "100.00", "Food", LocalDate.of(2026, 6, 2))));

        MonthlyReportResponse response = reportService.getMonthlyReport("2026-06");

        assertThat(response.getDailyTrend()).hasSize(30);
        assertThat(response.getDailyTrend().get(0).getDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(response.getDailyTrend().get(0).getIncome()).isEqualByComparingTo("0");
        assertThat(response.getDailyTrend().get(0).getExpense()).isEqualByComparingTo("0");
        assertThat(response.getDailyTrend().get(1).getDate()).isEqualTo(LocalDate.of(2026, 6, 2));
        assertThat(response.getDailyTrend().get(1).getIncome()).isEqualByComparingTo("1000.00");
        assertThat(response.getDailyTrend().get(1).getExpense()).isEqualByComparingTo("100.00");
        assertThat(response.getDailyTrend().get(1).getBalance()).isEqualByComparingTo("900.00");
        assertThat(response.getDailyTrend().get(1).getTransactionCount()).isEqualTo(2);
    }

    private Transaction transaction(
            Long id,
            TransactionType type,
            String amount,
            String categoryName,
            LocalDate transactionDate) {
        Category category = new Category();
        category.setId(id);
        category.setName(categoryName);
        category.setType(type == TransactionType.INCOME ? CategoryType.INCOME : CategoryType.EXPENSE);

        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setType(type);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setCategory(category);
        transaction.setTransactionDate(transactionDate);
        return transaction;
    }
}

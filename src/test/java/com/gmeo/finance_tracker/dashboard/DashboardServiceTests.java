package com.gmeo.finance_tracker.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gmeo.finance_tracker.category.Category;
import com.gmeo.finance_tracker.common.exception.BadRequestException;
import com.gmeo.finance_tracker.dashboard.dto.DashboardCategoryBreakdownResponse;
import com.gmeo.finance_tracker.dashboard.dto.DashboardSummaryResponse;
import com.gmeo.finance_tracker.dashboard.dto.DashboardTrendResponse;
import com.gmeo.finance_tracker.dashboard.enums.DashboardTrendGroupBy;
import com.gmeo.finance_tracker.security.CurrentUserService;
import com.gmeo.finance_tracker.transaction.Transaction;
import com.gmeo.finance_tracker.transaction.TransactionRepository;
import com.gmeo.finance_tracker.transaction.enums.TransactionType;
import com.gmeo.finance_tracker.user.User;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.jpa.domain.Specification;

class DashboardServiceTests {

    private TransactionRepository transactionRepository;
    private CurrentUserService currentUserService;
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        transactionRepository = Mockito.mock(TransactionRepository.class);
        currentUserService = Mockito.mock(CurrentUserService.class);
        dashboardService = new DashboardService(transactionRepository, currentUserService);

        User currentUser = new User();
        currentUser.setId(7L);
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
    }

    @Test
    void summaryIncludesIncomeExpenseBalanceAndTransactionCount() {
        when(transactionRepository.findAll(Mockito.<Specification<Transaction>>any()))
                .thenReturn(List.of(
                        transaction(TransactionType.INCOME, "2500.00"),
                        transaction(TransactionType.EXPENSE, "400.25"),
                        transaction(TransactionType.EXPENSE, "99.75")));

        DashboardSummaryResponse response = dashboardService.getSummary(null, null);

        assertThat(response.getTotalIncome()).isEqualByComparingTo("2500.00");
        assertThat(response.getTotalExpense()).isEqualByComparingTo("500.00");
        assertThat(response.getBalance()).isEqualByComparingTo("2000.00");
        assertThat(response.getTransactionCount()).isEqualTo(3);
    }

    @Test
    void summaryUsesProvidedDateRange() {
        LocalDate fromDate = LocalDate.of(2026, 5, 1);
        LocalDate toDate = LocalDate.of(2026, 5, 31);
        when(transactionRepository.findAll(Mockito.<Specification<Transaction>>any()))
                .thenReturn(List.of(transaction(TransactionType.EXPENSE, "25.50")));

        dashboardService.getSummary(fromDate, toDate);

        Specification<Transaction> specification = captureSpecification();
        Root<Transaction> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Path<Object> userPath = mock(Path.class);
        Path<Long> userIdPath = mock(Path.class);
        Path<LocalDate> transactionDatePath = mock(Path.class);
        when(root.get("user")).thenReturn(userPath);
        when(userPath.<Long>get("id")).thenReturn(userIdPath);
        when(root.<LocalDate>get("transactionDate")).thenReturn(transactionDatePath);

        specification.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).greaterThanOrEqualTo(transactionDatePath, fromDate);
        verify(criteriaBuilder).lessThanOrEqualTo(transactionDatePath, toDate);
    }

    @Test
    void emptySummaryReturnsZeroTotals() {
        when(transactionRepository.findAll(Mockito.<Specification<Transaction>>any())).thenReturn(List.of());

        DashboardSummaryResponse response = dashboardService.getSummary(null, null);

        assertThat(response.getTotalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getTotalExpense()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getTransactionCount()).isZero();
    }

    @Test
    void summaryIsScopedToCurrentUser() {
        when(transactionRepository.findAll(Mockito.<Specification<Transaction>>any())).thenReturn(List.of());

        dashboardService.getSummary(null, null);

        Specification<Transaction> specification = captureSpecification();
        Root<Transaction> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Path<Object> userPath = mock(Path.class);
        Path<Long> userIdPath = mock(Path.class);
        when(root.get("user")).thenReturn(userPath);
        when(userPath.<Long>get("id")).thenReturn(userIdPath);

        specification.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).equal(userIdPath, 7L);
    }

    @Test
    void invalidDateRangeIsRejected() {
        LocalDate fromDate = LocalDate.of(2026, 6, 1);
        LocalDate toDate = LocalDate.of(2026, 5, 31);

        assertThatThrownBy(() -> dashboardService.getSummary(fromDate, toDate))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("fromDate must be on or before toDate");
    }

    @Test
    void categoryBreakdownGroupsTransactionsAndSortsByTotalAmountDescending() {
        when(transactionRepository.findAll(Mockito.<Specification<Transaction>>any()))
                .thenReturn(List.of(
                        transaction(1L, "Food", TransactionType.EXPENSE, "100.00"),
                        transaction(2L, "Transport", TransactionType.EXPENSE, "90.00"),
                        transaction(1L, "Food", TransactionType.EXPENSE, "50.50")));

        DashboardCategoryBreakdownResponse response = dashboardService.getCategoryBreakdown(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                TransactionType.EXPENSE);

        assertThat(response.getTotalAmount()).isEqualByComparingTo("240.50");
        assertThat(response.getTotalTransactionCount()).isEqualTo(3);
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getCategoryName()).isEqualTo("Food");
        assertThat(response.getItems().get(0).getTotalAmount()).isEqualByComparingTo("150.50");
        assertThat(response.getItems().get(0).getTransactionCount()).isEqualTo(2);
        assertThat(response.getItems().get(1).getCategoryName()).isEqualTo("Transport");
        assertThat(response.getItems().get(1).getTotalAmount()).isEqualByComparingTo("90.00");
    }

    @Test
    void categoryBreakdownFiltersByDateRangeInclusively() {
        LocalDate fromDate = LocalDate.of(2026, 6, 1);
        LocalDate toDate = LocalDate.of(2026, 6, 30);
        when(transactionRepository.findAll(Mockito.<Specification<Transaction>>any())).thenReturn(List.of());

        dashboardService.getCategoryBreakdown(fromDate, toDate, TransactionType.EXPENSE);

        Specification<Transaction> specification = captureSpecification();
        Root<Transaction> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Path<Object> userPath = mock(Path.class);
        Path<Long> userIdPath = mock(Path.class);
        Path<TransactionType> typePath = mock(Path.class);
        Path<LocalDate> transactionDatePath = mock(Path.class);
        when(root.get("user")).thenReturn(userPath);
        when(userPath.<Long>get("id")).thenReturn(userIdPath);
        when(root.<TransactionType>get("type")).thenReturn(typePath);
        when(root.<LocalDate>get("transactionDate")).thenReturn(transactionDatePath);

        specification.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).greaterThanOrEqualTo(transactionDatePath, fromDate);
        verify(criteriaBuilder).lessThanOrEqualTo(transactionDatePath, toDate);
    }

    @Test
    void categoryBreakdownFiltersByTransactionType() {
        when(transactionRepository.findAll(Mockito.<Specification<Transaction>>any())).thenReturn(List.of());

        dashboardService.getCategoryBreakdown(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                TransactionType.INCOME);

        Specification<Transaction> specification = captureSpecification();
        Root<Transaction> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Path<Object> userPath = mock(Path.class);
        Path<Long> userIdPath = mock(Path.class);
        Path<TransactionType> typePath = mock(Path.class);
        Path<LocalDate> transactionDatePath = mock(Path.class);
        when(root.get("user")).thenReturn(userPath);
        when(userPath.<Long>get("id")).thenReturn(userIdPath);
        when(root.<TransactionType>get("type")).thenReturn(typePath);
        when(root.<LocalDate>get("transactionDate")).thenReturn(transactionDatePath);

        specification.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).equal(typePath, TransactionType.INCOME);
    }

    @Test
    void categoryBreakdownIsScopedToCurrentUser() {
        when(transactionRepository.findAll(Mockito.<Specification<Transaction>>any())).thenReturn(List.of());

        dashboardService.getCategoryBreakdown(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                TransactionType.EXPENSE);

        Specification<Transaction> specification = captureSpecification();
        Root<Transaction> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Path<Object> userPath = mock(Path.class);
        Path<Long> userIdPath = mock(Path.class);
        Path<TransactionType> typePath = mock(Path.class);
        Path<LocalDate> transactionDatePath = mock(Path.class);
        when(root.get("user")).thenReturn(userPath);
        when(userPath.<Long>get("id")).thenReturn(userIdPath);
        when(root.<TransactionType>get("type")).thenReturn(typePath);
        when(root.<LocalDate>get("transactionDate")).thenReturn(transactionDatePath);

        specification.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).equal(userIdPath, 7L);
    }

    @Test
    void emptyCategoryBreakdownReturnsZeroTotalsAndEmptyItems() {
        when(transactionRepository.findAll(Mockito.<Specification<Transaction>>any())).thenReturn(List.of());

        DashboardCategoryBreakdownResponse response = dashboardService.getCategoryBreakdown(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                TransactionType.EXPENSE);

        assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getTotalTransactionCount()).isZero();
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    void invalidCategoryBreakdownDateRangeIsRejected() {
        LocalDate fromDate = LocalDate.of(2026, 7, 1);
        LocalDate toDate = LocalDate.of(2026, 6, 30);

        assertThatThrownBy(() -> dashboardService.getCategoryBreakdown(fromDate, toDate, TransactionType.EXPENSE))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("fromDate must be on or before toDate");
    }

    @Test
    void trendGroupsByMonthAndCalculatesTotals() {
        when(transactionRepository.findAll(Mockito.<Specification<Transaction>>any()))
                .thenReturn(List.of(
                        datedTransaction(LocalDate.of(2026, 1, 5), TransactionType.INCOME, "1000.00"),
                        datedTransaction(LocalDate.of(2026, 1, 12), TransactionType.EXPENSE, "250.25"),
                        datedTransaction(LocalDate.of(2026, 1, 20), TransactionType.EXPENSE, "49.75")));

        DashboardTrendResponse response = dashboardService.getTrend(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                DashboardTrendGroupBy.MONTH);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getPeriod()).isEqualTo("2026-01");
        assertThat(response.getItems().get(0).getIncomeAmount()).isEqualByComparingTo("1000.00");
        assertThat(response.getItems().get(0).getExpenseAmount()).isEqualByComparingTo("300.00");
        assertThat(response.getItems().get(0).getBalance()).isEqualByComparingTo("700.00");
        assertThat(response.getItems().get(0).getTransactionCount()).isEqualTo(3);
    }

    @Test
    void trendGroupsByDay() {
        when(transactionRepository.findAll(Mockito.<Specification<Transaction>>any()))
                .thenReturn(List.of(
                        datedTransaction(LocalDate.of(2026, 6, 1), TransactionType.INCOME, "100.00"),
                        datedTransaction(LocalDate.of(2026, 6, 2), TransactionType.EXPENSE, "25.00")));

        DashboardTrendResponse response = dashboardService.getTrend(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 2),
                DashboardTrendGroupBy.DAY);

        assertThat(response.getItems()).extracting(item -> item.getPeriod())
                .containsExactly("2026-06-01", "2026-06-02");
        assertThat(response.getItems().get(0).getIncomeAmount()).isEqualByComparingTo("100.00");
        assertThat(response.getItems().get(1).getExpenseAmount()).isEqualByComparingTo("25.00");
    }

    @Test
    void trendIncludesEmptyPeriods() {
        when(transactionRepository.findAll(Mockito.<Specification<Transaction>>any()))
                .thenReturn(List.of(datedTransaction(
                        LocalDate.of(2026, 1, 10),
                        TransactionType.INCOME,
                        "100.00")));

        DashboardTrendResponse response = dashboardService.getTrend(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31),
                DashboardTrendGroupBy.MONTH);

        assertThat(response.getItems()).extracting(item -> item.getPeriod())
                .containsExactly("2026-01", "2026-02", "2026-03");
        assertThat(response.getItems().get(1).getIncomeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getItems().get(1).getExpenseAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getItems().get(1).getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getItems().get(1).getTransactionCount()).isZero();
    }

    @Test
    void trendFiltersByDateRangeInclusively() {
        LocalDate fromDate = LocalDate.of(2026, 6, 1);
        LocalDate toDate = LocalDate.of(2026, 6, 30);
        when(transactionRepository.findAll(Mockito.<Specification<Transaction>>any())).thenReturn(List.of());

        dashboardService.getTrend(fromDate, toDate, DashboardTrendGroupBy.MONTH);

        Specification<Transaction> specification = captureSpecification();
        Root<Transaction> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Path<Object> userPath = mock(Path.class);
        Path<Long> userIdPath = mock(Path.class);
        Path<LocalDate> transactionDatePath = mock(Path.class);
        when(root.get("user")).thenReturn(userPath);
        when(userPath.<Long>get("id")).thenReturn(userIdPath);
        when(root.<LocalDate>get("transactionDate")).thenReturn(transactionDatePath);

        specification.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).greaterThanOrEqualTo(transactionDatePath, fromDate);
        verify(criteriaBuilder).lessThanOrEqualTo(transactionDatePath, toDate);
    }

    @Test
    void trendIsScopedToCurrentUser() {
        when(transactionRepository.findAll(Mockito.<Specification<Transaction>>any())).thenReturn(List.of());

        dashboardService.getTrend(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                DashboardTrendGroupBy.MONTH);

        Specification<Transaction> specification = captureSpecification();
        Root<Transaction> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Path<Object> userPath = mock(Path.class);
        Path<Long> userIdPath = mock(Path.class);
        Path<LocalDate> transactionDatePath = mock(Path.class);
        when(root.get("user")).thenReturn(userPath);
        when(userPath.<Long>get("id")).thenReturn(userIdPath);
        when(root.<LocalDate>get("transactionDate")).thenReturn(transactionDatePath);

        specification.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).equal(userIdPath, 7L);
    }

    @Test
    void invalidTrendDateRangeIsRejected() {
        LocalDate fromDate = LocalDate.of(2026, 7, 1);
        LocalDate toDate = LocalDate.of(2026, 6, 30);

        assertThatThrownBy(() -> dashboardService.getTrend(fromDate, toDate, DashboardTrendGroupBy.MONTH))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("fromDate must be on or before toDate");
    }

    @SuppressWarnings("unchecked")
    private Specification<Transaction> captureSpecification() {
        ArgumentCaptor<Specification<Transaction>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(transactionRepository).findAll(captor.capture());
        return captor.getValue();
    }

    private Transaction transaction(TransactionType type, String amount) {
        Transaction transaction = new Transaction();
        transaction.setType(type);
        transaction.setAmount(new BigDecimal(amount));
        return transaction;
    }

    private Transaction transaction(Long categoryId, String categoryName, TransactionType type, String amount) {
        Category category = new Category();
        category.setId(categoryId);
        category.setName(categoryName);

        Transaction transaction = transaction(type, amount);
        transaction.setCategory(category);
        return transaction;
    }

    private Transaction datedTransaction(LocalDate date, TransactionType type, String amount) {
        Transaction transaction = transaction(type, amount);
        transaction.setTransactionDate(date);
        return transaction;
    }
}

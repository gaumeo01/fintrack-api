package com.gmeo.finance_tracker.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gmeo.finance_tracker.common.exception.BadRequestException;
import com.gmeo.finance_tracker.dashboard.dto.DashboardSummaryResponse;
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
}

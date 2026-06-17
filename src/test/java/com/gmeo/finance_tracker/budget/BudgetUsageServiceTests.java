package com.gmeo.finance_tracker.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gmeo.finance_tracker.budget.dto.BudgetUsageItemResponse;
import com.gmeo.finance_tracker.budget.dto.BudgetUsageResponse;
import com.gmeo.finance_tracker.category.Category;
import com.gmeo.finance_tracker.category.enums.CategoryType;
import com.gmeo.finance_tracker.security.CurrentUserService;
import com.gmeo.finance_tracker.transaction.TransactionRepository;
import com.gmeo.finance_tracker.transaction.enums.TransactionType;
import com.gmeo.finance_tracker.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BudgetUsageServiceTests {

    private BudgetRepository budgetRepository;
    private TransactionRepository transactionRepository;
    private CurrentUserService currentUserService;
    private BudgetUsageService budgetUsageService;
    private User currentUser;

    @BeforeEach
    void setUp() {
        budgetRepository = Mockito.mock(BudgetRepository.class);
        transactionRepository = Mockito.mock(TransactionRepository.class);
        currentUserService = Mockito.mock(CurrentUserService.class);
        budgetUsageService = new BudgetUsageService(budgetRepository, transactionRepository, currentUserService);

        currentUser = new User();
        currentUser.setId(7L);
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
    }

    @Test
    void returnsEmptyItemsWhenNoBudgetsExistForMonth() {
        when(budgetRepository.findAllByUserIdAndMonthAndCategoryType(
                7L,
                LocalDate.of(2026, 6, 1),
                CategoryType.EXPENSE))
                .thenReturn(List.of());

        BudgetUsageResponse response = budgetUsageService.getBudgetUsage("2026-06");

        assertThat(response.getMonth()).isEqualTo("2026-06");
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    void calculatesUsageAndSortsByUsagePercentDescending() {
        Budget foodBudget = budget(1L, "Food", "300.00");
        Budget transportBudget = budget(2L, "Transport", "100.00");
        when(budgetRepository.findAllByUserIdAndMonthAndCategoryType(
                7L,
                LocalDate.of(2026, 6, 1),
                CategoryType.EXPENSE))
                .thenReturn(List.of(foodBudget, transportBudget));
        when(transactionRepository.sumAmountsByCategory(
                eq(7L),
                eq(TransactionType.EXPENSE),
                eq(LocalDate.of(2026, 6, 1)),
                eq(LocalDate.of(2026, 7, 1)),
                eq(List.of(1L, 2L))))
                .thenReturn(List.of(spending(1L, "240.00"), spending(2L, "83.335")));

        BudgetUsageResponse response = budgetUsageService.getBudgetUsage("2026-06");

        assertThat(response.getItems()).hasSize(2);

        BudgetUsageItemResponse first = response.getItems().get(0);
        assertThat(first.getCategoryId()).isEqualTo(2L);
        assertThat(first.getSpentAmount()).isEqualByComparingTo("83.335");
        assertThat(first.getRemainingAmount()).isEqualByComparingTo("16.665");
        assertThat(first.getUsagePercent()).isEqualByComparingTo("83.34");
        assertThat(first.isOverBudget()).isFalse();

        BudgetUsageItemResponse second = response.getItems().get(1);
        assertThat(second.getCategoryId()).isEqualTo(1L);
        assertThat(second.getBudgetAmount()).isEqualByComparingTo("300.00");
        assertThat(second.getSpentAmount()).isEqualByComparingTo("240.00");
        assertThat(second.getRemainingAmount()).isEqualByComparingTo("60.00");
        assertThat(second.getUsagePercent()).isEqualByComparingTo("80.00");
        assertThat(second.isOverBudget()).isFalse();

        verify(transactionRepository).sumAmountsByCategory(
                7L,
                TransactionType.EXPENSE,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 7, 1),
                List.of(1L, 2L));
    }

    @Test
    void marksOverBudgetWhenSpentAmountExceedsBudgetAmount() {
        Budget foodBudget = budget(1L, "Food", "300.00");
        when(budgetRepository.findAllByUserIdAndMonthAndCategoryType(
                7L,
                LocalDate.of(2026, 6, 1),
                CategoryType.EXPENSE))
                .thenReturn(List.of(foodBudget));
        when(transactionRepository.sumAmountsByCategory(
                eq(7L),
                eq(TransactionType.EXPENSE),
                eq(LocalDate.of(2026, 6, 1)),
                eq(LocalDate.of(2026, 7, 1)),
                eq(List.of(1L))))
                .thenReturn(List.of(spending(1L, "350.00")));

        BudgetUsageItemResponse item = budgetUsageService.getBudgetUsage("2026-06").getItems().get(0);

        assertThat(item.getRemainingAmount()).isEqualByComparingTo("-50.00");
        assertThat(item.getUsagePercent()).isEqualByComparingTo("116.67");
        assertThat(item.isOverBudget()).isTrue();
    }

    private Budget budget(Long categoryId, String categoryName, String amount) {
        Category category = new Category();
        category.setId(categoryId);
        category.setName(categoryName);
        category.setType(CategoryType.EXPENSE);

        Budget budget = new Budget();
        budget.setId(categoryId);
        budget.setCategory(category);
        budget.setUser(currentUser);
        budget.setAmount(new BigDecimal(amount));
        budget.setMonth(LocalDate.of(2026, 6, 1));
        budget.setCreatedAt(LocalDateTime.of(2026, 6, 1, 10, 0));
        budget.setUpdatedAt(LocalDateTime.of(2026, 6, 1, 10, 0));
        return budget;
    }

    private TransactionRepository.CategorySpendingTotal spending(Long categoryId, String spentAmount) {
        return new TransactionRepository.CategorySpendingTotal() {
            @Override
            public Long getCategoryId() {
                return categoryId;
            }

            @Override
            public BigDecimal getSpentAmount() {
                return new BigDecimal(spentAmount);
            }
        };
    }
}

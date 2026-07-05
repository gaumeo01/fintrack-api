package com.gmeo.finance_tracker.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gmeo.finance_tracker.budget.dto.BudgetRequest;
import com.gmeo.finance_tracker.budget.dto.BudgetResponse;
import com.gmeo.finance_tracker.budget.dto.BudgetUsageResponse;
import com.gmeo.finance_tracker.category.Category;
import com.gmeo.finance_tracker.category.CategoryRepository;
import com.gmeo.finance_tracker.category.enums.CategoryType;
import com.gmeo.finance_tracker.common.exception.BadRequestException;
import com.gmeo.finance_tracker.common.exception.ResourceNotFoundException;
import com.gmeo.finance_tracker.security.CurrentUserService;
import com.gmeo.finance_tracker.transaction.TransactionRepository;
import com.gmeo.finance_tracker.transaction.enums.TransactionType;
import com.gmeo.finance_tracker.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class BudgetServiceTests {

    private BudgetRepository budgetRepository;
    private CategoryRepository categoryRepository;
    private TransactionRepository transactionRepository;
    private CurrentUserService currentUserService;
    private BudgetService budgetService;
    private User currentUser;

    @BeforeEach
    void setUp() {
        budgetRepository = Mockito.mock(BudgetRepository.class);
        categoryRepository = Mockito.mock(CategoryRepository.class);
        transactionRepository = Mockito.mock(TransactionRepository.class);
        currentUserService = Mockito.mock(CurrentUserService.class);
        budgetService = new BudgetService(
                budgetRepository,
                categoryRepository,
                transactionRepository,
                currentUserService);

        currentUser = new User();
        currentUser.setId(7L);
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
    }

    @Test
    void createBudgetAssignsCurrentUserAndOwnedExpenseCategory() {
        Category category = category(1L, "Food", CategoryType.EXPENSE);
        when(categoryRepository.findByIdAndUserId(1L, 7L)).thenReturn(Optional.of(category));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget budget = invocation.getArgument(0);
            budget.setId(10L);
            budget.setCreatedAt(LocalDateTime.of(2026, 6, 1, 10, 0));
            budget.setUpdatedAt(LocalDateTime.of(2026, 6, 1, 10, 0));
            return budget;
        });

        BudgetResponse response = budgetService.createBudget(request());

        ArgumentCaptor<Budget> budgetCaptor = ArgumentCaptor.forClass(Budget.class);
        verify(budgetRepository).save(budgetCaptor.capture());
        assertThat(budgetCaptor.getValue().getUser()).isEqualTo(currentUser);
        assertThat(budgetCaptor.getValue().getCategory()).isEqualTo(category);
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void createBudgetRejectsAnotherUsersCategory() {
        when(categoryRepository.findByIdAndUserId(1L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.createBudget(request()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found with id: 1");

        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void createBudgetRejectsNonExpenseCategory() {
        when(categoryRepository.findByIdAndUserId(1L, 7L))
                .thenReturn(Optional.of(category(1L, "Salary", CategoryType.INCOME)));

        assertThatThrownBy(() -> budgetService.createBudget(request()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Budget category must be an EXPENSE category");

        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void createBudgetRejectsReversedDateRange() {
        BudgetRequest request = request();
        request.setStartDate(LocalDate.of(2026, 6, 30));
        request.setEndDate(LocalDate.of(2026, 6, 1));

        assertThatThrownBy(() -> budgetService.createBudget(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("startDate must be on or before endDate");

        verifyNoCategoryLookupOrSave();
    }

    @Test
    void getAllBudgetsListsOnlyCurrentUsersBudgets() {
        when(budgetRepository.findAllByUserId(7L)).thenReturn(List.of(budget(10L)));

        List<BudgetResponse> response = budgetService.getAllBudgets();

        verify(budgetRepository).findAllByUserId(7L);
        assertThat(response).extracting(BudgetResponse::getId).containsExactly(10L);
    }

    @Test
    void getBudgetByIdCannotAccessAnotherUsersBudget() {
        when(budgetRepository.findByIdAndUserId(10L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.getBudgetById(10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Budget not found with id: 10");
    }

    @Test
    void updateBudgetCannotAccessAnotherUsersBudget() {
        when(budgetRepository.findByIdAndUserId(10L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.updateBudget(10L, request()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Budget not found with id: 10");

        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void updateBudgetUpdatesOwnedBudget() {
        Budget budget = budget(10L);
        Category category = category(2L, "Transport", CategoryType.EXPENSE);
        BudgetRequest request = request();
        request.setCategoryId(2L);
        request.setAmount(new BigDecimal("250.00"));

        when(budgetRepository.findByIdAndUserId(10L, 7L)).thenReturn(Optional.of(budget));
        when(categoryRepository.findByIdAndUserId(2L, 7L)).thenReturn(Optional.of(category));
        when(budgetRepository.save(budget)).thenReturn(budget);

        BudgetResponse response = budgetService.updateBudget(10L, request);

        assertThat(budget.getCategory()).isEqualTo(category);
        assertThat(budget.getAmount()).isEqualByComparingTo("250.00");
        assertThat(response.getCategoryId()).isEqualTo(2L);
    }

    @Test
    void deleteBudgetCannotAccessAnotherUsersBudget() {
        when(budgetRepository.findByIdAndUserId(10L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.deleteBudget(10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Budget not found with id: 10");

        verify(budgetRepository, never()).delete(any(Budget.class));
    }

    @Test
    void deleteBudgetDeletesOwnedBudget() {
        Budget budget = budget(10L);
        when(budgetRepository.findByIdAndUserId(10L, 7L)).thenReturn(Optional.of(budget));

        budgetService.deleteBudget(10L);

        verify(budgetRepository).delete(budget);
    }

    @Test
    void usageCalculatesSpentRemainingPercentageAndExceeded() {
        Budget budget = budget(10L);
        budget.setAmount(new BigDecimal("100.00"));
        when(budgetRepository.findByIdAndUserId(10L, 7L)).thenReturn(Optional.of(budget));
        when(transactionRepository.sumAmountByUserIdAndCategoryIdAndTypeAndDateRange(
                7L,
                1L,
                TransactionType.EXPENSE,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)))
                .thenReturn(new BigDecimal("125.50"));

        BudgetUsageResponse response = budgetService.getBudgetUsage(10L);

        assertThat(response.getBudgetId()).isEqualTo(10L);
        assertThat(response.getCategoryId()).isEqualTo(1L);
        assertThat(response.getLimitAmount()).isEqualByComparingTo("100.00");
        assertThat(response.getSpentAmount()).isEqualByComparingTo("125.50");
        assertThat(response.getRemainingAmount()).isEqualByComparingTo("-25.50");
        assertThat(response.getUsagePercentage()).isEqualByComparingTo("125.50");
        assertThat(response.isExceeded()).isTrue();
    }

    private void verifyNoCategoryLookupOrSave() {
        verify(categoryRepository, never()).findByIdAndUserId(any(), any());
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    private BudgetRequest request() {
        BudgetRequest request = new BudgetRequest();
        request.setCategoryId(1L);
        request.setAmount(new BigDecimal("100.00"));
        request.setStartDate(LocalDate.of(2026, 6, 1));
        request.setEndDate(LocalDate.of(2026, 6, 30));
        return request;
    }

    private Budget budget(Long id) {
        Budget budget = new Budget();
        budget.setId(id);
        budget.setUser(currentUser);
        budget.setCategory(category(1L, "Food", CategoryType.EXPENSE));
        budget.setAmount(new BigDecimal("100.00"));
        budget.setStartDate(LocalDate.of(2026, 6, 1));
        budget.setEndDate(LocalDate.of(2026, 6, 30));
        budget.setCreatedAt(LocalDateTime.of(2026, 6, 1, 10, 0));
        budget.setUpdatedAt(LocalDateTime.of(2026, 6, 1, 10, 0));
        return budget;
    }

    private Category category(Long id, String name, CategoryType type) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setType(type);
        return category;
    }
}

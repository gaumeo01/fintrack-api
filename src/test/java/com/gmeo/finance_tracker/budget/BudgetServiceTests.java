package com.gmeo.finance_tracker.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gmeo.finance_tracker.budget.dto.BudgetRequest;
import com.gmeo.finance_tracker.budget.dto.BudgetResponse;
import com.gmeo.finance_tracker.category.Category;
import com.gmeo.finance_tracker.category.CategoryRepository;
import com.gmeo.finance_tracker.category.enums.CategoryType;
import com.gmeo.finance_tracker.common.exception.BadRequestException;
import com.gmeo.finance_tracker.common.exception.DuplicateResourceException;
import com.gmeo.finance_tracker.common.exception.ResourceNotFoundException;
import com.gmeo.finance_tracker.security.CurrentUserService;
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
    private CurrentUserService currentUserService;
    private BudgetService budgetService;
    private User currentUser;

    @BeforeEach
    void setUp() {
        budgetRepository = Mockito.mock(BudgetRepository.class);
        categoryRepository = Mockito.mock(CategoryRepository.class);
        currentUserService = Mockito.mock(CurrentUserService.class);
        budgetService = new BudgetService(budgetRepository, categoryRepository, currentUserService);

        currentUser = new User();
        currentUser.setId(7L);
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
    }

    @Test
    void createBudgetAssignsCurrentUserAndExpenseCategory() {
        Category category = category(1L, "Food", CategoryType.EXPENSE);
        Budget savedBudget = budget(10L, category, new BigDecimal("300.00"), LocalDate.of(2026, 6, 1));
        when(categoryRepository.findByIdAndUserId(1L, 7L)).thenReturn(Optional.of(category));
        when(budgetRepository.existsByUserIdAndCategoryIdAndMonth(7L, 1L, LocalDate.of(2026, 6, 1)))
                .thenReturn(false);
        when(budgetRepository.save(any(Budget.class))).thenReturn(savedBudget);

        BudgetResponse response = budgetService.createBudget(request(1L, "300.00", "2026-06"));

        ArgumentCaptor<Budget> budgetCaptor = ArgumentCaptor.forClass(Budget.class);
        verify(budgetRepository).save(budgetCaptor.capture());
        assertThat(budgetCaptor.getValue().getUser()).isEqualTo(currentUser);
        assertThat(budgetCaptor.getValue().getCategory()).isEqualTo(category);
        assertThat(budgetCaptor.getValue().getMonth()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getAmount()).isEqualByComparingTo("300.00");
        assertThat(response.getMonth()).isEqualTo("2026-06");
    }

    @Test
    void listBudgetsReturnsCurrentUsersBudgetsForMonth() {
        Budget budget = budget(10L, category(1L, "Food", CategoryType.EXPENSE), new BigDecimal("300.00"),
                LocalDate.of(2026, 6, 1));
        when(budgetRepository.findAllByUserIdAndMonthOrderByCategoryNameAsc(7L, LocalDate.of(2026, 6, 1)))
                .thenReturn(List.of(budget));

        List<BudgetResponse> response = budgetService.getBudgets("2026-06");

        verify(budgetRepository).findAllByUserIdAndMonthOrderByCategoryNameAsc(7L, LocalDate.of(2026, 6, 1));
        assertThat(response).extracting(BudgetResponse::getId).containsExactly(10L);
    }

    @Test
    void updateBudgetChangesCategoryAmountAndMonth() {
        Category oldCategory = category(1L, "Food", CategoryType.EXPENSE);
        Category newCategory = category(2L, "Transport", CategoryType.EXPENSE);
        Budget existingBudget = budget(10L, oldCategory, new BigDecimal("300.00"), LocalDate.of(2026, 6, 1));
        Budget savedBudget = budget(10L, newCategory, new BigDecimal("150.00"), LocalDate.of(2026, 7, 1));

        when(budgetRepository.findByIdAndUserId(10L, 7L)).thenReturn(Optional.of(existingBudget));
        when(categoryRepository.findByIdAndUserId(2L, 7L)).thenReturn(Optional.of(newCategory));
        when(budgetRepository.existsByUserIdAndCategoryIdAndMonthAndIdNot(
                7L,
                2L,
                LocalDate.of(2026, 7, 1),
                10L))
                .thenReturn(false);
        when(budgetRepository.save(existingBudget)).thenReturn(savedBudget);

        BudgetResponse response = budgetService.updateBudget(10L, request(2L, "150.00", "2026-07"));

        assertThat(existingBudget.getCategory()).isEqualTo(newCategory);
        assertThat(existingBudget.getAmount()).isEqualByComparingTo("150.00");
        assertThat(existingBudget.getMonth()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(response.getCategoryName()).isEqualTo("Transport");
        assertThat(response.getMonth()).isEqualTo("2026-07");
    }

    @Test
    void deleteBudgetDeletesOwnedBudget() {
        Budget existingBudget = budget(10L, category(1L, "Food", CategoryType.EXPENSE), new BigDecimal("300.00"),
                LocalDate.of(2026, 6, 1));
        when(budgetRepository.findByIdAndUserId(10L, 7L)).thenReturn(Optional.of(existingBudget));

        budgetService.deleteBudget(10L);

        verify(budgetRepository).delete(existingBudget);
    }

    @Test
    void duplicateBudgetIsRejectedOnCreate() {
        Category category = category(1L, "Food", CategoryType.EXPENSE);
        when(categoryRepository.findByIdAndUserId(1L, 7L)).thenReturn(Optional.of(category));
        when(budgetRepository.existsByUserIdAndCategoryIdAndMonth(7L, 1L, LocalDate.of(2026, 6, 1)))
                .thenReturn(true);

        assertThatThrownBy(() -> budgetService.createBudget(request(1L, "300.00", "2026-06")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Budget already exists for this category and month");

        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void duplicateBudgetIsRejectedOnUpdate() {
        Category category = category(1L, "Food", CategoryType.EXPENSE);
        Budget existingBudget = budget(10L, category, new BigDecimal("300.00"), LocalDate.of(2026, 6, 1));
        when(budgetRepository.findByIdAndUserId(10L, 7L)).thenReturn(Optional.of(existingBudget));
        when(categoryRepository.findByIdAndUserId(1L, 7L)).thenReturn(Optional.of(category));
        when(budgetRepository.existsByUserIdAndCategoryIdAndMonthAndIdNot(
                7L,
                1L,
                LocalDate.of(2026, 6, 1),
                10L))
                .thenReturn(true);

        assertThatThrownBy(() -> budgetService.updateBudget(10L, request(1L, "400.00", "2026-06")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Budget already exists for this category and month");

        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void crossUserBudgetAccessIsRejectedAsNotFound() {
        when(budgetRepository.findByIdAndUserId(10L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.getBudgetById(10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Budget not found with id: 10");
    }

    @Test
    void crossUserCategoryIsRejectedAsNotFound() {
        when(categoryRepository.findByIdAndUserId(1L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.createBudget(request(1L, "300.00", "2026-06")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found with id: 1");

        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void incomeCategoryIsRejected() {
        Category category = category(1L, "Salary", CategoryType.INCOME);
        when(categoryRepository.findByIdAndUserId(1L, 7L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> budgetService.createBudget(request(1L, "300.00", "2026-06")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only EXPENSE categories can have budgets");

        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void invalidMonthIsRejected() {
        Category category = category(1L, "Food", CategoryType.EXPENSE);
        when(categoryRepository.findByIdAndUserId(1L, 7L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> budgetService.createBudget(request(1L, "300.00", "2026-13")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("month must use YYYY-MM format");

        verify(budgetRepository, never()).save(any(Budget.class));
    }

    private BudgetRequest request(Long categoryId, String amount, String month) {
        BudgetRequest request = new BudgetRequest();
        request.setCategoryId(categoryId);
        request.setAmount(new BigDecimal(amount));
        request.setMonth(month);
        return request;
    }

    private Budget budget(Long id, Category category, BigDecimal amount, LocalDate month) {
        Budget budget = new Budget();
        budget.setId(id);
        budget.setCategory(category);
        budget.setUser(currentUser);
        budget.setAmount(amount);
        budget.setMonth(month);
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

package com.gmeo.finance_tracker.budget;

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
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BudgetService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM")
            .withResolverStyle(ResolverStyle.STRICT);

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;

    public BudgetService(
            BudgetRepository budgetRepository,
            CategoryRepository categoryRepository,
            CurrentUserService currentUserService) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.currentUserService = currentUserService;
    }

    public BudgetResponse createBudget(BudgetRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Category category = findOwnedExpenseCategory(request.getCategoryId(), currentUser.getId());
        LocalDate month = parseMonth(request.getMonth());

        if (budgetRepository.existsByUserIdAndCategoryIdAndMonth(currentUser.getId(), category.getId(), month)) {
            throw new DuplicateResourceException("Budget already exists for this category and month");
        }

        Budget budget = new Budget();
        budget.setCategory(category);
        budget.setUser(currentUser);
        budget.setAmount(request.getAmount());
        budget.setMonth(month);

        Budget savedBudget = budgetRepository.save(budget);
        return mapToResponse(savedBudget);
    }

    public List<BudgetResponse> getBudgets(String month) {
        User currentUser = currentUserService.getCurrentUser();
        LocalDate budgetMonth = parseMonth(month);

        return budgetRepository.findAllByUserIdAndMonthOrderByCategoryNameAsc(currentUser.getId(), budgetMonth)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public BudgetResponse getBudgetById(Long id) {
        Budget budget = findOwnedBudget(id);

        return mapToResponse(budget);
    }

    public BudgetResponse updateBudget(Long id, BudgetRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Budget budget = findOwnedBudget(id, currentUser.getId());
        Category category = findOwnedExpenseCategory(request.getCategoryId(), currentUser.getId());
        LocalDate month = parseMonth(request.getMonth());

        if (budgetRepository.existsByUserIdAndCategoryIdAndMonthAndIdNot(
                currentUser.getId(),
                category.getId(),
                month,
                id)) {
            throw new DuplicateResourceException("Budget already exists for this category and month");
        }

        budget.setCategory(category);
        budget.setAmount(request.getAmount());
        budget.setMonth(month);

        Budget savedBudget = budgetRepository.save(budget);
        return mapToResponse(savedBudget);
    }

    public void deleteBudget(Long id) {
        Budget budget = findOwnedBudget(id);

        budgetRepository.delete(budget);
    }

    private Category findOwnedExpenseCategory(Long categoryId, Long userId) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
        if (category.getType() != CategoryType.EXPENSE) {
            throw new BadRequestException("Only EXPENSE categories can have budgets");
        }
        return category;
    }

    private Budget findOwnedBudget(Long id) {
        return findOwnedBudget(id, currentUserService.getCurrentUser().getId());
    }

    private Budget findOwnedBudget(Long id, Long userId) {
        return budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + id));
    }

    private LocalDate parseMonth(String month) {
        try {
            YearMonth yearMonth = YearMonth.parse(month, MONTH_FORMATTER);
            if (yearMonth.getYear() < 1) {
                throw new DateTimeParseException("Invalid year", month, 0);
            }
            return yearMonth.atDay(1);
        } catch (DateTimeParseException exception) {
            throw new BadRequestException("month must use YYYY-MM format");
        }
    }

    private BudgetResponse mapToResponse(Budget budget) {
        BudgetResponse response = new BudgetResponse();
        response.setId(budget.getId());
        response.setCategoryId(budget.getCategory().getId());
        response.setCategoryName(budget.getCategory().getName());
        response.setCategoryType(budget.getCategory().getType());
        response.setAmount(budget.getAmount());
        response.setMonth(YearMonth.from(budget.getMonth()).format(MONTH_FORMATTER));
        response.setCreatedAt(budget.getCreatedAt());
        response.setUpdatedAt(budget.getUpdatedAt());
        return response;
    }
}

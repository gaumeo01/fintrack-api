package com.gmeo.finance_tracker.budget;

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
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;

    public BudgetService(
            BudgetRepository budgetRepository,
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository,
            CurrentUserService currentUserService) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
    }

    public BudgetResponse createBudget(BudgetRequest request) {
        validateDateRange(request);
        User currentUser = currentUserService.getCurrentUser();
        Category category = findOwnedExpenseCategory(request.getCategoryId(), currentUser.getId());

        Budget budget = new Budget();
        budget.setUser(currentUser);
        budget.setCategory(category);
        budget.setAmount(request.getAmount());
        budget.setStartDate(request.getStartDate());
        budget.setEndDate(request.getEndDate());

        return mapToResponse(budgetRepository.save(budget));
    }

    public List<BudgetResponse> getAllBudgets() {
        User currentUser = currentUserService.getCurrentUser();
        return budgetRepository.findAllByUserId(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public BudgetResponse getBudgetById(Long id) {
        return mapToResponse(findOwnedBudget(id));
    }

    public BudgetResponse updateBudget(Long id, BudgetRequest request) {
        validateDateRange(request);
        User currentUser = currentUserService.getCurrentUser();
        Budget budget = findOwnedBudget(id, currentUser.getId());
        Category category = findOwnedExpenseCategory(request.getCategoryId(), currentUser.getId());

        budget.setCategory(category);
        budget.setAmount(request.getAmount());
        budget.setStartDate(request.getStartDate());
        budget.setEndDate(request.getEndDate());

        return mapToResponse(budgetRepository.save(budget));
    }

    public void deleteBudget(Long id) {
        Budget budget = findOwnedBudget(id);

        budgetRepository.delete(budget);
    }

    public BudgetUsageResponse getBudgetUsage(Long id) {
        User currentUser = currentUserService.getCurrentUser();
        Budget budget = findOwnedBudget(id, currentUser.getId());
        BigDecimal spentAmount = transactionRepository.sumAmountByUserIdAndCategoryIdAndTypeAndDateRange(
                currentUser.getId(),
                budget.getCategory().getId(),
                TransactionType.EXPENSE,
                budget.getStartDate(),
                budget.getEndDate());
        BigDecimal remainingAmount = budget.getAmount().subtract(spentAmount);
        BigDecimal usagePercentage = spentAmount
                .multiply(new BigDecimal("100"))
                .divide(budget.getAmount(), 2, RoundingMode.HALF_UP);

        BudgetUsageResponse response = new BudgetUsageResponse();
        response.setBudgetId(budget.getId());
        response.setCategoryId(budget.getCategory().getId());
        response.setCategoryName(budget.getCategory().getName());
        response.setLimitAmount(budget.getAmount());
        response.setSpentAmount(spentAmount);
        response.setRemainingAmount(remainingAmount);
        response.setUsagePercentage(usagePercentage);
        response.setStatus(resolveStatus(usagePercentage));
        response.setExceeded(spentAmount.compareTo(budget.getAmount()) > 0);
        response.setStartDate(budget.getStartDate());
        response.setEndDate(budget.getEndDate());
        return response;
    }

    private String resolveStatus(BigDecimal usagePercentage) {
        if (usagePercentage.compareTo(new BigDecimal("100")) > 0) {
            return "OVER_BUDGET";
        }
        if (usagePercentage.compareTo(new BigDecimal("80")) >= 0) {
            return "WARNING";
        }
        return "SAFE";
    }

    private void validateDateRange(BudgetRequest request) {
        if (request.getStartDate() != null
                && request.getEndDate() != null
                && request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException("startDate must be on or before endDate");
        }
    }

    private Category findOwnedExpenseCategory(Long categoryId, Long userId) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        if (category.getType() != CategoryType.EXPENSE) {
            throw new BadRequestException("Budget category must be an EXPENSE category");
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

    private BudgetResponse mapToResponse(Budget budget) {
        BudgetResponse response = new BudgetResponse();
        response.setId(budget.getId());
        response.setCategoryId(budget.getCategory().getId());
        response.setCategoryName(budget.getCategory().getName());
        response.setAmount(budget.getAmount());
        response.setStartDate(budget.getStartDate());
        response.setEndDate(budget.getEndDate());
        response.setCreatedAt(budget.getCreatedAt());
        response.setUpdatedAt(budget.getUpdatedAt());
        return response;
    }
}

package com.gmeo.finance_tracker.budget;

import com.gmeo.finance_tracker.budget.dto.BudgetUsageItemResponse;
import com.gmeo.finance_tracker.budget.dto.BudgetUsageResponse;
import com.gmeo.finance_tracker.category.enums.CategoryType;
import com.gmeo.finance_tracker.common.util.DateTimeUtils;
import com.gmeo.finance_tracker.security.CurrentUserService;
import com.gmeo.finance_tracker.transaction.TransactionRepository;
import com.gmeo.finance_tracker.transaction.enums.TransactionType;
import com.gmeo.finance_tracker.user.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class BudgetUsageService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;

    public BudgetUsageService(
            BudgetRepository budgetRepository,
            TransactionRepository transactionRepository,
            CurrentUserService currentUserService) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
    }

    public BudgetUsageResponse getBudgetUsage(String month) {
        User currentUser = currentUserService.getCurrentUser();
        LocalDate monthStart = DateTimeUtils.parseMonthStart(month);
        List<Budget> budgets = budgetRepository.findAllByUserIdAndMonthAndCategoryType(
                currentUser.getId(),
                monthStart,
                CategoryType.EXPENSE);

        BudgetUsageResponse response = new BudgetUsageResponse();
        response.setMonth(DateTimeUtils.formatMonth(monthStart));

        if (budgets.isEmpty()) {
            response.setItems(List.of());
            return response;
        }

        Map<Long, BigDecimal> spentByCategoryId = getSpentByCategoryId(
                currentUser.getId(),
                monthStart,
                budgets.stream()
                        .map(budget -> budget.getCategory().getId())
                        .toList());

        List<BudgetUsageItemResponse> items = budgets.stream()
                .map(budget -> mapToUsageItem(budget, spentByCategoryId.getOrDefault(
                        budget.getCategory().getId(),
                        BigDecimal.ZERO)))
                .sorted(Comparator.comparing(BudgetUsageItemResponse::getUsagePercent).reversed())
                .toList();

        response.setItems(items);
        return response;
    }

    private Map<Long, BigDecimal> getSpentByCategoryId(Long userId, LocalDate monthStart, List<Long> categoryIds) {
        LocalDate nextMonthStart = monthStart.plusMonths(1);
        return transactionRepository.sumAmountsByCategory(
                        userId,
                        TransactionType.EXPENSE,
                        monthStart,
                        nextMonthStart,
                        categoryIds)
                .stream()
                .collect(Collectors.toMap(
                        TransactionRepository.CategorySpendingTotal::getCategoryId,
                        TransactionRepository.CategorySpendingTotal::getSpentAmount,
                        BigDecimal::add));
    }

    private BudgetUsageItemResponse mapToUsageItem(Budget budget, BigDecimal spentAmount) {
        BigDecimal budgetAmount = budget.getAmount();
        BigDecimal remainingAmount = budgetAmount.subtract(spentAmount);
        BigDecimal usagePercent = spentAmount
                .multiply(ONE_HUNDRED)
                .divide(budgetAmount, 2, RoundingMode.HALF_UP);

        BudgetUsageItemResponse item = new BudgetUsageItemResponse();
        item.setCategoryId(budget.getCategory().getId());
        item.setCategoryName(budget.getCategory().getName());
        item.setBudgetAmount(budgetAmount);
        item.setSpentAmount(spentAmount);
        item.setRemainingAmount(remainingAmount);
        item.setUsagePercent(usagePercent);
        item.setOverBudget(spentAmount.compareTo(budgetAmount) > 0);
        return item;
    }
}

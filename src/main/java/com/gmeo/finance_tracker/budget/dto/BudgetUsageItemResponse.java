package com.gmeo.finance_tracker.budget.dto;

import com.gmeo.finance_tracker.budget.enums.BudgetUsageStatus;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BudgetUsageItemResponse {

    private Long categoryId;
    private String categoryName;
    private BigDecimal budgetAmount;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private BigDecimal usagePercent;
    private boolean overBudget;
    private BudgetUsageStatus status;
}

package com.gmeo.finance_tracker.budget.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BudgetUsageResponse {

    private Long budgetId;
    private Long categoryId;
    private String categoryName;
    private BigDecimal limitAmount;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private BigDecimal usagePercentage;
    private boolean exceeded;
    private LocalDate startDate;
    private LocalDate endDate;
}

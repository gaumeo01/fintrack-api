package com.gmeo.finance_tracker.budget.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BudgetResponse {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private BigDecimal limitAmount;
    private YearMonth month;
}

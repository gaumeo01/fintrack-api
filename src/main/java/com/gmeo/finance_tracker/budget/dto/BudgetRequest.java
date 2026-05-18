package com.gmeo.finance_tracker.budget.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BudgetRequest {

    private Long categoryId;
    private BigDecimal limitAmount;
    private YearMonth month;
}

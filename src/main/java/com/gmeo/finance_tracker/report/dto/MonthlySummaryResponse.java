package com.gmeo.finance_tracker.report.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MonthlySummaryResponse {

    private YearMonth month;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal balance;
}

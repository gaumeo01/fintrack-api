package com.gmeo.finance_tracker.dashboard.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardTrendItem {

    private String period;
    private BigDecimal incomeAmount;
    private BigDecimal expenseAmount;
    private BigDecimal balance;
    private long transactionCount;
}

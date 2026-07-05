package com.gmeo.finance_tracker.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MonthlyReportDailyTrendItem {

    private LocalDate date;
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal balance;
    private long transactionCount;
}

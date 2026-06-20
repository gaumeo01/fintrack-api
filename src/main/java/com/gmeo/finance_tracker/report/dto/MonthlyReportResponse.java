package com.gmeo.finance_tracker.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MonthlyReportResponse {

    private String month;
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal balance;
    private long transactionCount;
    private List<MonthlyReportCategoryItem> topExpenseCategories;
    private List<MonthlyReportCategoryItem> topIncomeCategories;
    private List<MonthlyReportDailyTrendItem> dailyTrend;
}

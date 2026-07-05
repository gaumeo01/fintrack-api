package com.gmeo.finance_tracker.report.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MonthlyReportCategoryItem {

    private Long categoryId;
    private String categoryName;
    private BigDecimal amount;
    private long transactionCount;
}

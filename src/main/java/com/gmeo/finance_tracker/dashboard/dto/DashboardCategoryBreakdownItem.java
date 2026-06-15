package com.gmeo.finance_tracker.dashboard.dto;

import com.gmeo.finance_tracker.transaction.enums.TransactionType;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardCategoryBreakdownItem {

    private Long categoryId;
    private String categoryName;
    private TransactionType type;
    private BigDecimal totalAmount;
    private long transactionCount;
}

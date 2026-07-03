package com.gmeo.finance_tracker.dashboard.dto;

import com.gmeo.finance_tracker.transaction.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardCategoryBreakdownResponse {

    private LocalDate fromDate;
    private LocalDate toDate;
    private TransactionType type;
    private BigDecimal totalAmount;
    private long totalTransactionCount;
    private List<DashboardCategoryBreakdownItem> items;
}

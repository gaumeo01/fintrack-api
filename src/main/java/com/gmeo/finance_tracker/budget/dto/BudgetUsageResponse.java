package com.gmeo.finance_tracker.budget.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BudgetUsageResponse {

    private String month;
    private List<BudgetUsageItemResponse> items;
}

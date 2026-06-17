package com.gmeo.finance_tracker.budget.dto;

import com.gmeo.finance_tracker.category.enums.CategoryType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BudgetResponse {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private CategoryType categoryType;
    private BigDecimal amount;
    private String month;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

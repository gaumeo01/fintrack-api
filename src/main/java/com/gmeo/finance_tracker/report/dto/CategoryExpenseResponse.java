package com.gmeo.finance_tracker.report.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryExpenseResponse {

    private String categoryName;
    private BigDecimal totalAmount;
    private BigDecimal percentage;
}

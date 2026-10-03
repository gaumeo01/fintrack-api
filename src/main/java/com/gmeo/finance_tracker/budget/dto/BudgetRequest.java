package com.gmeo.finance_tracker.budget.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BudgetRequest {

    @NotNull
    private Long categoryId;

    @NotNull
    @DecimalMin(value = "0.01")
    @jakarta.validation.constraints.Digits(integer = 17, fraction = 2)
    @io.swagger.v3.oas.annotations.media.Schema(description = "Exact positive decimal: up to 17 integer digits and 2 fractional digits. Excess scale is rejected, never rounded.", example = "12.34")
    private BigDecimal amount;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;
}

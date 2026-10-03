package com.gmeo.finance_tracker.recurring.dto;

import com.gmeo.finance_tracker.recurring.enums.RecurringTransactionFrequency;
import com.gmeo.finance_tracker.transaction.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecurringTransactionRequest {

    @NotNull
    private TransactionType type;

    @NotNull
    @DecimalMin(value = "0.01")
    @jakarta.validation.constraints.Digits(integer = 17, fraction = 2)
    @io.swagger.v3.oas.annotations.media.Schema(description = "Exact positive decimal: up to 17 integer digits and 2 fractional digits. Excess scale is rejected, never rounded.", example = "12.34")
    private BigDecimal amount;

    @NotNull
    private Long categoryId;

    @Size(max = 255)
    private String description;

    @NotNull
    private RecurringTransactionFrequency frequency;

    @NotNull
    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean active;
}

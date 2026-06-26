package com.gmeo.finance_tracker.transaction.dto;

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
public class TransactionRequest {

    @NotNull
    private TransactionType type;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @NotNull
    private Long categoryId;

    @Size(max = 255, message = "description must be at most 255 characters")
    private String description;

    @NotNull
    private LocalDate transactionDate;
}

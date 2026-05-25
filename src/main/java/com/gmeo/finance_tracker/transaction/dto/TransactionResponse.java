package com.gmeo.finance_tracker.transaction.dto;

import com.gmeo.finance_tracker.category.enums.CategoryType;
import com.gmeo.finance_tracker.transaction.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionResponse {

    private Long id;
    private TransactionType type;
    private BigDecimal amount;
    private Long categoryId;
    private String categoryName;
    private CategoryType categoryType;
    private String description;
    private LocalDate transactionDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

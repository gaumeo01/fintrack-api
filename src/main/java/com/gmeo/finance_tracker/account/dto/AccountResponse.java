package com.gmeo.finance_tracker.account.dto;

import com.gmeo.finance_tracker.account.enums.AccountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountResponse {

    private Long id;
    private String name;
    private AccountType type;
    private BigDecimal initialBalance;
    private BigDecimal currentBalance;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

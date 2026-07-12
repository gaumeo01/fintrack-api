package com.gmeo.finance_tracker.account.dto;

import com.gmeo.finance_tracker.account.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    private AccountType type;

    @NotNull
    private BigDecimal initialBalance;

    private BigDecimal currentBalance;

    private Boolean active;
}

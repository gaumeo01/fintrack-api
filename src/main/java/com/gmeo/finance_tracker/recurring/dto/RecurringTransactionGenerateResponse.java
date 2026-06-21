package com.gmeo.finance_tracker.recurring.dto;

import com.gmeo.finance_tracker.transaction.dto.TransactionResponse;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecurringTransactionGenerateResponse {

    private TransactionResponse transaction;
    private LocalDate nextRunDate;
}

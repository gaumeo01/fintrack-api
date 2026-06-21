package com.gmeo.finance_tracker.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransactionImportError {

    private int rowNumber;
    private String message;
}

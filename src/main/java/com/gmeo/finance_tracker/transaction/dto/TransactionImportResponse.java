package com.gmeo.finance_tracker.transaction.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransactionImportResponse {

    private int totalRows;
    private int successfulRows;
    private int failedRows;
    private List<TransactionImportError> errors;
}

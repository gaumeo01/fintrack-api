package com.gmeo.finance_tracker.recurring;

import com.gmeo.finance_tracker.recurring.dto.RecurringTransactionGenerateResponse;
import com.gmeo.finance_tracker.recurring.dto.RecurringTransactionRequest;
import com.gmeo.finance_tracker.recurring.dto.RecurringTransactionResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recurring-transactions")
public class RecurringTransactionController {

    private final RecurringTransactionService recurringTransactionService;

    public RecurringTransactionController(RecurringTransactionService recurringTransactionService) {
        this.recurringTransactionService = recurringTransactionService;
    }

    @PostMapping
    public ResponseEntity<RecurringTransactionResponse> createRecurringTransaction(
            @Valid @RequestBody RecurringTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recurringTransactionService.createRecurringTransaction(request));
    }

    @GetMapping
    public List<RecurringTransactionResponse> getRecurringTransactions() {
        return recurringTransactionService.getRecurringTransactions();
    }

    @GetMapping("/{id}")
    public RecurringTransactionResponse getRecurringTransaction(@PathVariable Long id) {
        return recurringTransactionService.getRecurringTransaction(id);
    }

    @PutMapping("/{id}")
    public RecurringTransactionResponse updateRecurringTransaction(
            @PathVariable Long id,
            @Valid @RequestBody RecurringTransactionRequest request) {
        return recurringTransactionService.updateRecurringTransaction(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecurringTransaction(@PathVariable Long id) {
        recurringTransactionService.deleteRecurringTransaction(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/generate")
    public RecurringTransactionGenerateResponse generateTransaction(@PathVariable Long id) {
        return recurringTransactionService.generateTransaction(id);
    }
}

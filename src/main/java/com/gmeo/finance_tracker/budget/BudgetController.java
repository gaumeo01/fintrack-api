package com.gmeo.finance_tracker.budget;

import com.gmeo.finance_tracker.budget.dto.BudgetRequest;
import com.gmeo.finance_tracker.budget.dto.BudgetResponse;
import com.gmeo.finance_tracker.budget.dto.BudgetUsageResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final BudgetUsageService budgetUsageService;

    public BudgetController(BudgetService budgetService, BudgetUsageService budgetUsageService) {
        this.budgetService = budgetService;
        this.budgetUsageService = budgetUsageService;
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(@Valid @RequestBody BudgetRequest request) {
        BudgetResponse response = budgetService.createBudget(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<BudgetResponse> getBudgets(@RequestParam String month) {
        return budgetService.getBudgets(month);
    }

    @GetMapping("/usage")
    public BudgetUsageResponse getBudgetUsage(@RequestParam String month) {
        return budgetUsageService.getBudgetUsage(month);
    }

    @GetMapping("/{id}")
    public BudgetResponse getBudgetById(@PathVariable Long id) {
        return budgetService.getBudgetById(id);
    }

    @PutMapping("/{id}")
    public BudgetResponse updateBudget(
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequest request) {
        return budgetService.updateBudget(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        budgetService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }
}

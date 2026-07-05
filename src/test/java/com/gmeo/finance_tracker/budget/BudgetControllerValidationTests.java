package com.gmeo.finance_tracker.budget;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gmeo.finance_tracker.auth.JwtService;
import com.gmeo.finance_tracker.budget.dto.BudgetRequest;
import com.gmeo.finance_tracker.budget.dto.BudgetResponse;
import com.gmeo.finance_tracker.budget.dto.BudgetUsageResponse;
import com.gmeo.finance_tracker.common.exception.BadRequestException;
import com.gmeo.finance_tracker.common.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BudgetController.class)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class BudgetControllerValidationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BudgetService budgetService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void createBudgetReturnsCreatedForValidRequest() throws Exception {
        BudgetResponse response = new BudgetResponse();
        response.setId(1L);
        response.setCategoryId(2L);
        response.setCategoryName("Food");
        response.setAmount(new BigDecimal("100.00"));
        response.setStartDate(LocalDate.of(2026, 6, 1));
        response.setEndDate(LocalDate.of(2026, 6, 30));

        when(budgetService.createBudget(any(BudgetRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": 2,
                                  "amount": 100.00,
                                  "startDate": "2026-06-01",
                                  "endDate": "2026-06-30"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.categoryId").value(2))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.startDate").value("2026-06-01"))
                .andExpect(jsonPath("$.endDate").value("2026-06-30"));
    }

    @Test
    void createBudgetReturnsBadRequestForInvalidAmount() throws Exception {
        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": 2,
                                  "amount": 0,
                                  "startDate": "2026-06-01",
                                  "endDate": "2026-06-30"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.amount").exists());

        verifyNoInteractions(budgetService);
    }

    @Test
    void createBudgetReturnsBadRequestForMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 100.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.categoryId").exists())
                .andExpect(jsonPath("$.validationErrors.startDate").exists())
                .andExpect(jsonPath("$.validationErrors.endDate").exists());

        verifyNoInteractions(budgetService);
    }

    @Test
    void createBudgetReturnsBadRequestForReversedDateRange() throws Exception {
        BudgetRequest request = new BudgetRequest();
        request.setCategoryId(2L);
        request.setAmount(new BigDecimal("100.00"));
        request.setStartDate(LocalDate.of(2026, 6, 30));
        request.setEndDate(LocalDate.of(2026, 6, 1));
        when(budgetService.createBudget(any(BudgetRequest.class)))
                .thenThrow(new BadRequestException("startDate must be on or before endDate"));

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": 2,
                                  "amount": 100.00,
                                  "startDate": "2026-06-30",
                                  "endDate": "2026-06-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("startDate must be on or before endDate"));
    }

    @Test
    void getBudgetUsageReturnsExpectedResponse() throws Exception {
        BudgetUsageResponse response = new BudgetUsageResponse();
        response.setBudgetId(1L);
        response.setCategoryId(2L);
        response.setCategoryName("Food");
        response.setLimitAmount(new BigDecimal("100.00"));
        response.setSpentAmount(new BigDecimal("75.00"));
        response.setRemainingAmount(new BigDecimal("25.00"));
        response.setUsagePercentage(new BigDecimal("75.00"));
        response.setExceeded(false);
        response.setStartDate(LocalDate.of(2026, 6, 1));
        response.setEndDate(LocalDate.of(2026, 6, 30));
        when(budgetService.getBudgetUsage(1L)).thenReturn(response);

        mockMvc.perform(get("/api/budgets/1/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetId").value(1))
                .andExpect(jsonPath("$.categoryId").value(2))
                .andExpect(jsonPath("$.limitAmount").value(100.00))
                .andExpect(jsonPath("$.spentAmount").value(75.00))
                .andExpect(jsonPath("$.remainingAmount").value(25.00))
                .andExpect(jsonPath("$.usagePercentage").value(75.00))
                .andExpect(jsonPath("$.exceeded").value(false));
    }
}

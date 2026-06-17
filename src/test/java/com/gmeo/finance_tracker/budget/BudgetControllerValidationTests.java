package com.gmeo.finance_tracker.budget;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gmeo.finance_tracker.auth.JwtService;
import com.gmeo.finance_tracker.budget.dto.BudgetRequest;
import com.gmeo.finance_tracker.budget.dto.BudgetResponse;
import com.gmeo.finance_tracker.category.enums.CategoryType;
import com.gmeo.finance_tracker.common.exception.BadRequestException;
import com.gmeo.finance_tracker.common.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.util.List;
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
        BudgetResponse response = response();
        when(budgetService.createBudget(any(BudgetRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": 1,
                                  "amount": 300.00,
                                  "month": "2026-06"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.categoryName").value("Food"))
                .andExpect(jsonPath("$.categoryType").value("EXPENSE"))
                .andExpect(jsonPath("$.amount").value(300.00))
                .andExpect(jsonPath("$.month").value("2026-06"));
    }

    @Test
    void getBudgetsReturnsBudgetsForMonth() throws Exception {
        when(budgetService.getBudgets("2026-06")).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/budgets")
                        .param("month", "2026-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].month").value("2026-06"));

        verify(budgetService).getBudgets("2026-06");
    }

    @Test
    void createBudgetReturnsBadRequestForInvalidAmount() throws Exception {
        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": 1,
                                  "amount": 0,
                                  "month": "2026-06"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.amount").exists());

        verifyNoInteractions(budgetService);
    }

    @Test
    void createBudgetReturnsBadRequestForMissingCategoryId() throws Exception {
        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 300.00,
                                  "month": "2026-06"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.categoryId").exists());

        verifyNoInteractions(budgetService);
    }

    @Test
    void createBudgetReturnsBadRequestForMalformedMonth() throws Exception {
        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": 1,
                                  "amount": 300.00,
                                  "month": "2026/06"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.month").exists());

        verifyNoInteractions(budgetService);
    }

    @Test
    void getBudgetsReturnsBadRequestForInvalidMonth() throws Exception {
        when(budgetService.getBudgets("2026-13"))
                .thenThrow(new BadRequestException("month must use YYYY-MM format"));

        mockMvc.perform(get("/api/budgets")
                        .param("month", "2026-13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("month must use YYYY-MM format"));
    }

    @Test
    void getBudgetsReturnsBadRequestForMissingMonth() throws Exception {
        mockMvc.perform(get("/api/budgets"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.month").exists());

        verifyNoInteractions(budgetService);
    }

    private BudgetResponse response() {
        BudgetResponse response = new BudgetResponse();
        response.setId(10L);
        response.setCategoryId(1L);
        response.setCategoryName("Food");
        response.setCategoryType(CategoryType.EXPENSE);
        response.setAmount(new BigDecimal("300.00"));
        response.setMonth("2026-06");
        return response;
    }
}

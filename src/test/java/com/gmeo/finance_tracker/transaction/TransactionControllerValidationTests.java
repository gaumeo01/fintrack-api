package com.gmeo.finance_tracker.transaction;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gmeo.finance_tracker.category.enums.CategoryType;
import com.gmeo.finance_tracker.common.exception.GlobalExceptionHandler;
import com.gmeo.finance_tracker.transaction.dto.TransactionRequest;
import com.gmeo.finance_tracker.transaction.dto.TransactionResponse;
import com.gmeo.finance_tracker.transaction.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransactionController.class)
@Import(GlobalExceptionHandler.class)
class TransactionControllerValidationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void createTransactionReturnsCreatedForValidRequest() throws Exception {
        TransactionResponse response = new TransactionResponse();
        response.setId(1L);
        response.setType(TransactionType.EXPENSE);
        response.setAmount(new BigDecimal("25.50"));
        response.setCategoryId(1L);
        response.setCategoryName("Food");
        response.setCategoryType(CategoryType.EXPENSE);
        response.setDescription("Lunch");
        response.setTransactionDate(LocalDate.of(2026, 5, 20));

        when(transactionService.createTransaction(any(TransactionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "EXPENSE",
                                  "amount": 25.50,
                                  "categoryId": 1,
                                  "description": "Lunch",
                                  "transactionDate": "2026-05-20"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.amount").value(25.50))
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.categoryName").value("Food"))
                .andExpect(jsonPath("$.categoryType").value("EXPENSE"));
    }

    @Test
    void createTransactionReturnsBadRequestForInvalidAmount() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "EXPENSE",
                                  "amount": 0,
                                  "categoryId": 1,
                                  "description": "Lunch",
                                  "transactionDate": "2026-05-20"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.amount").exists());

        verifyNoInteractions(transactionService);
    }

    @Test
    void createTransactionReturnsBadRequestForMissingCategoryId() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "EXPENSE",
                                  "amount": 25.50,
                                  "description": "Lunch",
                                  "transactionDate": "2026-05-20"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.categoryId").exists());

        verifyNoInteractions(transactionService);
    }

    @Test
    void createTransactionReturnsBadRequestForMissingType() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 25.50,
                                  "categoryId": 1,
                                  "description": "Lunch",
                                  "transactionDate": "2026-05-20"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.type").exists());

        verifyNoInteractions(transactionService);
    }
}

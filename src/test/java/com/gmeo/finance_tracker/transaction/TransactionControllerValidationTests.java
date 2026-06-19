package com.gmeo.finance_tracker.transaction;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gmeo.finance_tracker.category.enums.CategoryType;
import com.gmeo.finance_tracker.auth.JwtService;
import com.gmeo.finance_tracker.common.dto.PageResponse;
import com.gmeo.finance_tracker.common.exception.GlobalExceptionHandler;
import com.gmeo.finance_tracker.transaction.dto.TransactionRequest;
import com.gmeo.finance_tracker.transaction.dto.TransactionResponse;
import com.gmeo.finance_tracker.transaction.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransactionController.class)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class TransactionControllerValidationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void getTransactionsReturnsPaginatedFilteredTransactions() throws Exception {
        TransactionResponse transaction = new TransactionResponse();
        transaction.setId(1L);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setAmount(new BigDecimal("25.50"));
        transaction.setCategoryId(1L);
        transaction.setCategoryName("Food");
        transaction.setCategoryType(CategoryType.EXPENSE);
        transaction.setDescription("Lunch");
        transaction.setTransactionDate(LocalDate.of(2026, 5, 20));

        PageResponse<TransactionResponse> response = new PageResponse<>(
                List.of(transaction),
                0,
                10,
                1,
                1,
                true);

        when(transactionService.getTransactions(
                eq(TransactionType.EXPENSE),
                eq(1L),
                eq(LocalDate.of(2026, 5, 1)),
                eq(LocalDate.of(2026, 5, 31)),
                eq(new BigDecimal("10")),
                eq(new BigDecimal("100")),
                any(Pageable.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/transactions")
                        .param("type", "EXPENSE")
                        .param("categoryId", "1")
                        .param("fromDate", "2026-05-01")
                        .param("toDate", "2026-05-31")
                        .param("minAmount", "10")
                        .param("maxAmount", "100")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "transactionDate,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].type").value("EXPENSE"))
                .andExpect(jsonPath("$.content[0].categoryId").value(1))
                .andExpect(jsonPath("$.content[0].categoryName").value("Food"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.last").value(true));

        verify(transactionService).getTransactions(
                eq(TransactionType.EXPENSE),
                eq(1L),
                eq(LocalDate.of(2026, 5, 1)),
                eq(LocalDate.of(2026, 5, 31)),
                eq(new BigDecimal("10")),
                eq(new BigDecimal("100")),
                any(Pageable.class));
    }

    @Test
    void getTransactionsAcceptsSortingByTransactionDateDescending() throws Exception {
        when(transactionService.getTransactions(
                eq(TransactionType.EXPENSE),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 10, 0, 0, true));

        mockMvc.perform(get("/api/transactions")
                        .param("type", "EXPENSE")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "transactionDate,desc"))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<Pageable> pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(transactionService).getTransactions(
                eq(TransactionType.EXPENSE),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                pageableCaptor.capture());

        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("transactionDate");
        org.assertj.core.api.Assertions.assertThat(order).isNotNull();
        org.assertj.core.api.Assertions.assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void exportTransactionsReturnsCsvAttachment() throws Exception {
        String csv = "id,type,amount,categoryId,categoryName,description,transactionDate,createdAt,updatedAt\n"
                + "1,EXPENSE,25.50,1,Food,Lunch,2026-05-20,2026-05-20T10:00,2026-05-20T10:00";

        when(transactionService.exportTransactions(
                eq(TransactionType.EXPENSE),
                eq(1L),
                eq(LocalDate.of(2026, 5, 1)),
                eq(LocalDate.of(2026, 5, 31)),
                eq(new BigDecimal("10")),
                eq(new BigDecimal("100"))))
                .thenReturn(csv);

        mockMvc.perform(get("/api/transactions/export")
                        .param("type", "EXPENSE")
                        .param("categoryId", "1")
                        .param("fromDate", "2026-05-01")
                        .param("toDate", "2026-05-31")
                        .param("minAmount", "10")
                        .param("maxAmount", "100"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("text/csv")))
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename=\"transactions-export.csv\""))
                .andExpect(content().string(csv));

        verify(transactionService).exportTransactions(
                eq(TransactionType.EXPENSE),
                eq(1L),
                eq(LocalDate.of(2026, 5, 1)),
                eq(LocalDate.of(2026, 5, 31)),
                eq(new BigDecimal("10")),
                eq(new BigDecimal("100")));
    }

    @Test
    void exportTransactionsReturnsHeaderOnlyCsv() throws Exception {
        String csv = "id,type,amount,categoryId,categoryName,description,transactionDate,createdAt,updatedAt";
        when(transactionService.exportTransactions(
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null)))
                .thenReturn(csv);

        mockMvc.perform(get("/api/transactions/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("text/csv")))
                .andExpect(content().string(csv));

        verify(transactionService).exportTransactions(
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null));
    }

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

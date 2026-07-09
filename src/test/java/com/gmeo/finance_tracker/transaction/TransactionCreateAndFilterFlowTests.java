package com.gmeo.finance_tracker.transaction;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gmeo.finance_tracker.auth.JwtService;
import com.gmeo.finance_tracker.account.AccountRepository;
import com.gmeo.finance_tracker.budget.BudgetRepository;
import com.gmeo.finance_tracker.category.CategoryRepository;
import com.gmeo.finance_tracker.recurring.RecurringTransactionRepository;
import com.jayway.jsonpath.JsonPath;
import com.gmeo.finance_tracker.user.User;
import com.gmeo.finance_tracker.user.UserRepository;
import com.gmeo.finance_tracker.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionCreateAndFilterFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String accessToken;

    @BeforeEach
    void setUpAuthentication() {
        transactionRepository.deleteAll();
        recurringTransactionRepository.deleteAll();
        budgetRepository.deleteAll();
        accountRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("flow@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setFullName("Flow Test User");
        user.setRole(UserRole.USER);
        userRepository.save(user);

        accessToken = jwtService.generateAccessToken(user.getEmail());
    }

    @Test
    void createCategoryCreateTransactionAndFilterTransactions() throws Exception {
        Long categoryId = createExpenseCategory();

        mockMvc.perform(post("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "EXPENSE",
                                  "amount": 25.50,
                                  "categoryId": %d,
                                  "description": "Lunch",
                                  "transactionDate": "2026-05-20"
                                }
                                """.formatted(categoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.amount").value(25.50))
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.categoryName").value("Food"))
                .andExpect(jsonPath("$.categoryType").value("EXPENSE"))
                .andExpect(jsonPath("$.description").value("Lunch"))
                .andExpect(jsonPath("$.transactionDate").value("2026-05-20"));

        mockMvc.perform(get("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .param("type", "EXPENSE")
                        .param("categoryId", String.valueOf(categoryId))
                        .param("fromDate", "2026-05-01")
                        .param("toDate", "2026-05-31")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "transactionDate,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].type").value("EXPENSE"))
                .andExpect(jsonPath("$.content[0].categoryId").value(categoryId))
                .andExpect(jsonPath("$.content[0].categoryName").value("Food"))
                .andExpect(jsonPath("$.content[0].categoryType").value("EXPENSE"))
                .andExpect(jsonPath("$.content[0].transactionDate").value("2026-05-20"));
    }

    @Test
    void createTransactionReturnsNotFoundForNonExistingCategoryId() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "EXPENSE",
                                  "amount": 25.50,
                                  "categoryId": 999999,
                                  "description": "Lunch",
                                  "transactionDate": "2026-05-20"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found with id: 999999"));
    }

    @Test
    void createTransactionReturnsBadRequestForNullCategoryId() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "EXPENSE",
                                  "amount": 25.50,
                                  "categoryId": null,
                                  "description": "Lunch",
                                  "transactionDate": "2026-05-20"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.categoryId").exists());
    }

    @Test
    void createTransactionAcceptsDescriptionAtMaximumLength() throws Exception {
        Long categoryId = createExpenseCategory();
        String description = "a".repeat(255);

        mockMvc.perform(post("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "EXPENSE",
                                  "amount": 25.50,
                                  "categoryId": %d,
                                  "description": "%s",
                                  "transactionDate": "2026-05-20"
                                }
                                """.formatted(categoryId, description)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value(description));
    }

    @Test
    void createTransactionReturnsBadRequestForMalformedRequest() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "EXPENSE",
                                  "amount":
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    private Long createExpenseCategory() throws Exception {
        MvcResult categoryResult = mockMvc.perform(post("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Food",
                                  "type": "EXPENSE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        Number categoryId = JsonPath.read(categoryResult.getResponse().getContentAsString(), "$.id");
        return categoryId.longValue();
    }
}

package com.gmeo.finance_tracker.budget;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gmeo.finance_tracker.account.AccountRepository;
import com.gmeo.finance_tracker.auth.JwtService;
import com.gmeo.finance_tracker.category.CategoryRepository;
import com.gmeo.finance_tracker.transaction.TransactionRepository;
import com.gmeo.finance_tracker.user.User;
import com.gmeo.finance_tracker.user.UserRepository;
import com.gmeo.finance_tracker.user.enums.UserRole;
import com.jayway.jsonpath.JsonPath;
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
class BudgetUsageFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String accessToken;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        budgetRepository.deleteAll();
        accountRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("budget-flow@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setFullName("Budget Flow User");
        user.setRole(UserRole.USER);
        userRepository.save(user);

        accessToken = jwtService.generateAccessToken(user.getEmail());
    }

    @Test
    void getBudgetUsageCalculatesFromOwnedExpenseTransactions() throws Exception {
        Long categoryId = createCategory("Food", "EXPENSE");
        Long otherCategoryId = createCategory("Transport", "EXPENSE");
        Long budgetId = createBudget(categoryId, "100.00", "2026-06-01", "2026-06-30");
        createTransaction(categoryId, "30.00", "2026-06-01");
        createTransaction(categoryId, "45.50", "2026-06-30");
        createTransaction(categoryId, "20.00", "2026-07-01");
        createTransaction(otherCategoryId, "10.00", "2026-06-15");

        mockMvc.perform(get("/api/budgets/{id}/usage", budgetId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetId").value(budgetId))
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.categoryName").value("Food"))
                .andExpect(jsonPath("$.limitAmount").value(100.00))
                .andExpect(jsonPath("$.spentAmount").value(75.50))
                .andExpect(jsonPath("$.remainingAmount").value(24.50))
                .andExpect(jsonPath("$.usagePercentage").value(75.50))
                .andExpect(jsonPath("$.status").value("SAFE"))
                .andExpect(jsonPath("$.exceeded").value(false))
                .andExpect(jsonPath("$.startDate").value("2026-06-01"))
                .andExpect(jsonPath("$.endDate").value("2026-06-30"));
    }

    @Test
    void createBudgetRejectsIncomeCategory() throws Exception {
        Long categoryId = createCategory("Salary", "INCOME");

        mockMvc.perform(post("/api/budgets")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(budgetJson(categoryId, "100.00", "2026-06-01", "2026-06-30")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Budget category must be an EXPENSE category"));
    }

    private Long createCategory(String name, String type) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "type": "%s"
                                }
                                """.formatted(name, type)))
                .andExpect(status().isCreated())
                .andReturn();

        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private Long createBudget(
            Long categoryId,
            String amount,
            String startDate,
            String endDate) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/budgets")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(budgetJson(categoryId, amount, startDate, endDate)))
                .andExpect(status().isCreated())
                .andReturn();

        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private void createTransaction(Long categoryId, String amount, String transactionDate) throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "EXPENSE",
                                  "amount": %s,
                                  "categoryId": %d,
                                  "description": "Budget usage",
                                  "transactionDate": "%s"
                                }
                                """.formatted(amount, categoryId, transactionDate)))
                .andExpect(status().isCreated());
    }

    private String budgetJson(
            Long categoryId,
            String amount,
            String startDate,
            String endDate) {
        return """
                {
                  "categoryId": %d,
                  "amount": %s,
                  "startDate": "%s",
                  "endDate": "%s"
                }
                """.formatted(categoryId, amount, startDate, endDate);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

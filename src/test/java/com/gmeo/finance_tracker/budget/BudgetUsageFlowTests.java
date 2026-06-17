package com.gmeo.finance_tracker.budget;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    private BudgetRepository budgetRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String userAToken;
    private String userBToken;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        budgetRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        userAToken = createUserAndToken("budget-usage-a@example.com", "Budget Usage A");
        userBToken = createUserAndToken("budget-usage-b@example.com", "Budget Usage B");
    }

    @Test
    void returnsBudgetUsageForAuthenticatedUserFromExpenseTransactionsInSameCategoryAndMonth() throws Exception {
        Long userAFoodCategoryId = createCategory(userAToken, "Food", "EXPENSE");
        Long userBFoodCategoryId = createCategory(userBToken, "Food", "EXPENSE");
        createBudget(userAToken, userAFoodCategoryId, "300.00", "2026-06");
        createBudget(userBToken, userBFoodCategoryId, "100.00", "2026-06");

        createTransaction(userAToken, userAFoodCategoryId, "EXPENSE", "240.00", "2026-06-15");
        createTransaction(userAToken, userAFoodCategoryId, "INCOME", "50.00", "2026-06-16");
        createTransaction(userAToken, userAFoodCategoryId, "EXPENSE", "60.00", "2026-07-01");
        createTransaction(userBToken, userBFoodCategoryId, "EXPENSE", "70.00", "2026-06-15");

        mockMvc.perform(get("/api/budgets/usage")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken))
                        .param("month", "2026-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-06"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].categoryId").value(userAFoodCategoryId))
                .andExpect(jsonPath("$.items[0].categoryName").value("Food"))
                .andExpect(jsonPath("$.items[0].budgetAmount").value(300.00))
                .andExpect(jsonPath("$.items[0].spentAmount").value(240.00))
                .andExpect(jsonPath("$.items[0].remainingAmount").value(60.00))
                .andExpect(jsonPath("$.items[0].usagePercent").value(80.00))
                .andExpect(jsonPath("$.items[0].overBudget").value(false));
    }

    private String createUserAndToken(String email, String fullName) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setFullName(fullName);
        user.setRole(UserRole.USER);
        userRepository.save(user);
        return jwtService.generateAccessToken(email);
    }

    private Long createCategory(String token, String name, String type) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
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

    private void createBudget(String token, Long categoryId, String amount, String month) throws Exception {
        mockMvc.perform(post("/api/budgets")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": %d,
                                  "amount": %s,
                                  "month": "%s"
                                }
                                """.formatted(categoryId, amount, month)))
                .andExpect(status().isCreated());
    }

    private void createTransaction(
            String token,
            Long categoryId,
            String type,
            String amount,
            String transactionDate) throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "%s",
                                  "amount": %s,
                                  "categoryId": %d,
                                  "description": "Budget usage fixture",
                                  "transactionDate": "%s"
                                }
                                """.formatted(type, amount, categoryId, transactionDate)))
                .andExpect(status().isCreated());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

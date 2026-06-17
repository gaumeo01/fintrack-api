package com.gmeo.finance_tracker.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gmeo.finance_tracker.auth.JwtService;
import com.gmeo.finance_tracker.budget.BudgetRepository;
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
class UserDataIsolationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BudgetRepository budgetRepository;

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

        userAToken = createUserAndToken("user-a@example.com", "User A");
        userBToken = createUserAndToken("user-b@example.com", "User B");
    }

    @Test
    void userCannotAccessAnotherUsersCategory() throws Exception {
        Long categoryId = createCategory(userBToken, "User B Food");

        mockMvc.perform(get("/api/categories/{id}", categoryId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("Changed")))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/categories/{id}", categoryId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void userCannotAccessAnotherUsersTransaction() throws Exception {
        Long categoryId = createCategory(userBToken, "User B Food");
        Long transactionId = createTransaction(userBToken, categoryId, "User B Lunch");

        mockMvc.perform(get("/api/transactions/{id}", transactionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/transactions/{id}", transactionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionJson(categoryId, "Changed")))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/transactions/{id}", transactionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotCreateTransactionWithAnotherUsersCategory() throws Exception {
        Long categoryId = createCategory(userBToken, "User B Food");

        mockMvc.perform(post("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionJson(categoryId, "Cross-user attempt")))
                .andExpect(status().isNotFound());
    }

    @Test
    void transactionFilteringOnlyReturnsAuthenticatedUsersData() throws Exception {
        Long userACategoryId = createCategory(userAToken, "User A Food");
        Long userBCategoryId = createCategory(userBToken, "User B Food");
        createTransaction(userAToken, userACategoryId, "User A Lunch");
        createTransaction(userBToken, userBCategoryId, "User B Lunch");

        mockMvc.perform(get("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken))
                        .param("type", "EXPENSE")
                        .param("fromDate", "2026-05-01")
                        .param("toDate", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].description").value("User A Lunch"));
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

    private Long createCategory(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson(name)))
                .andExpect(status().isCreated())
                .andReturn();

        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private Long createTransaction(String token, Long categoryId, String description) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionJson(categoryId, description)))
                .andExpect(status().isCreated())
                .andReturn();

        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private String transactionJson(Long categoryId, String description) {
        return """
                {
                  "type": "EXPENSE",
                  "amount": 25.50,
                  "categoryId": %d,
                  "description": "%s",
                  "transactionDate": "2026-05-20"
                }
                """.formatted(categoryId, description);
    }

    private String categoryJson(String name) {
        return """
                {
                  "name": "%s",
                  "type": "EXPENSE"
                }
                """.formatted(name);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

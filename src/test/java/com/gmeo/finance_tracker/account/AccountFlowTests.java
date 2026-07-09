package com.gmeo.finance_tracker.account;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gmeo.finance_tracker.auth.JwtService;
import com.gmeo.finance_tracker.budget.BudgetRepository;
import com.gmeo.finance_tracker.category.CategoryRepository;
import com.gmeo.finance_tracker.recurring.RecurringTransactionRepository;
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
class AccountFlowTests {

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

    private String userAToken;
    private String userBToken;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        transactionRepository.deleteAll();
        recurringTransactionRepository.deleteAll();
        budgetRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        userAToken = createUserAndToken("account-a@example.com", "Account A");
        userBToken = createUserAndToken("account-b@example.com", "Account B");
    }

    @Test
    void createListGetUpdateAndDeleteAccount() throws Exception {
        Long accountId = createAccount(userAToken, "Cash", "CASH");

        mockMvc.perform(get("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(accountId))
                .andExpect(jsonPath("$[0].name").value("Cash"))
                .andExpect(jsonPath("$[0].active").value(true));

        mockMvc.perform(get("/api/accounts/{id}", accountId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("CASH"));

        mockMvc.perform(put("/api/accounts/{id}", accountId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson("Main Bank", "BANK", "100.00", "125.50", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Main Bank"))
                .andExpect(jsonPath("$.type").value("BANK"))
                .andExpect(jsonPath("$.currentBalance").value(125.50))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(delete("/api/accounts/{id}", accountId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void listOnlyReturnsAuthenticatedUsersAccounts() throws Exception {
        createAccount(userAToken, "User A Cash", "CASH");
        createAccount(userBToken, "User B Bank", "BANK");

        mockMvc.perform(get("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("User A Cash"));
    }

    @Test
    void crossUserAccessIsRejected() throws Exception {
        Long accountId = createAccount(userBToken, "User B Bank", "BANK");

        mockMvc.perform(get("/api/accounts/{id}", accountId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/accounts/{id}", accountId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson("Changed", "BANK", "100.00", "100.00", true)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/accounts/{id}", accountId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isNotFound());
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

    private Long createAccount(String token, String name, String type) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson(name, type, "100.00", null, true)))
                .andExpect(status().isCreated())
                .andReturn();

        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private String accountJson(
            String name,
            String type,
            String initialBalance,
            String currentBalance,
            boolean active) {
        String currentBalanceJson = currentBalance == null ? "" : """
                  "currentBalance": %s,
                """.formatted(currentBalance);
        return """
                {
                  "name": "%s",
                  "type": "%s",
                  "initialBalance": %s,
                %s  "active": %s
                }
                """.formatted(name, type, initialBalance, currentBalanceJson, active);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

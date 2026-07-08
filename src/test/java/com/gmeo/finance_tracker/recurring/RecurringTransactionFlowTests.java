package com.gmeo.finance_tracker.recurring;

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
import java.time.LocalDate;
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
class RecurringTransactionFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    @Autowired
    private TransactionRepository transactionRepository;

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
        transactionRepository.deleteAll();
        recurringTransactionRepository.deleteAll();
        budgetRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        userAToken = createUserAndToken("recurring-a@example.com", "Recurring A");
        userBToken = createUserAndToken("recurring-b@example.com", "Recurring B");
    }

    @Test
    void createListGetUpdateAndDeleteRecurringTransaction() throws Exception {
        Long categoryId = createCategory(userAToken, "Food", "EXPENSE");

        Long recurringId = createRecurring(userAToken, categoryId, "EXPENSE", "MONTHLY", "2026-06-01", true);

        mockMvc.perform(get("/api/recurring-transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(recurringId))
                .andExpect(jsonPath("$[0].nextRunDate").value("2026-06-01"))
                .andExpect(jsonPath("$[0].active").value(true));

        mockMvc.perform(get("/api/recurring-transactions/{id}", recurringId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(categoryId));

        mockMvc.perform(put("/api/recurring-transactions/{id}", recurringId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recurringJson(categoryId, "EXPENSE", "WEEKLY", "2026-06-01", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frequency").value("WEEKLY"))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(delete("/api/recurring-transactions/{id}", recurringId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void listOnlyReturnsAuthenticatedUsersRecurringTransactions() throws Exception {
        Long userACategoryId = createCategory(userAToken, "User A Food", "EXPENSE");
        Long userBCategoryId = createCategory(userBToken, "User B Food", "EXPENSE");
        createRecurring(userAToken, userACategoryId, "EXPENSE", "MONTHLY", "2026-06-01", true);
        createRecurring(userBToken, userBCategoryId, "EXPENSE", "MONTHLY", "2026-06-01", true);

        mockMvc.perform(get("/api/recurring-transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].categoryName").value("User A Food"));
    }

    @Test
    void crossUserAccessIsRejected() throws Exception {
        Long userBCategoryId = createCategory(userBToken, "User B Food", "EXPENSE");
        Long recurringId = createRecurring(userBToken, userBCategoryId, "EXPENSE", "MONTHLY", "2026-06-01", true);

        mockMvc.perform(get("/api/recurring-transactions/{id}", recurringId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/recurring-transactions/{id}", recurringId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recurringJson(userBCategoryId, "EXPENSE", "MONTHLY", "2026-06-01", true)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/recurring-transactions/{id}", recurringId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/recurring-transactions/{id}/generate", recurringId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createRejectsAnotherUsersCategoryAndMismatchedCategoryType() throws Exception {
        Long userBCategoryId = createCategory(userBToken, "User B Food", "EXPENSE");
        Long incomeCategoryId = createCategory(userAToken, "Salary", "INCOME");

        mockMvc.perform(post("/api/recurring-transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recurringJson(userBCategoryId, "EXPENSE", "MONTHLY", "2026-06-01", true)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/recurring-transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recurringJson(incomeCategoryId, "EXPENSE", "MONTHLY", "2026-06-01", true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Category type must match transaction type"));
    }

    @Test
    void generateCreatesTransactionAndAdvancesNextRunDate() throws Exception {
        Long categoryId = createCategory(userAToken, "Food", "EXPENSE");
        LocalDate startDate = LocalDate.now().minusDays(1);
        Long recurringId = createRecurring(userAToken, categoryId, "EXPENSE", "DAILY", startDate.toString(), true);

        mockMvc.perform(post("/api/recurring-transactions/{id}/generate", recurringId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.transactionDate").value(startDate.toString()))
                .andExpect(jsonPath("$.transaction.categoryId").value(categoryId))
                .andExpect(jsonPath("$.nextRunDate").value(startDate.plusDays(1).toString()));
    }

    @Test
    void generateRejectsInactiveFutureAndEndedRecurringTransactions() throws Exception {
        Long categoryId = createCategory(userAToken, "Food", "EXPENSE");
        Long inactiveId = createRecurring(userAToken, categoryId, "EXPENSE", "DAILY", LocalDate.now().minusDays(1).toString(), false);
        Long futureId = createRecurring(userAToken, categoryId, "EXPENSE", "DAILY", LocalDate.now().plusDays(1).toString(), true);
        Long endedId = createRecurringWithEndDate(
                userAToken,
                categoryId,
                LocalDate.now().minusDays(2).toString(),
                LocalDate.now().minusDays(2).toString());
        mockMvc.perform(post("/api/recurring-transactions/{id}/generate", endedId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/recurring-transactions/{id}/generate", inactiveId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Recurring transaction is inactive"));

        mockMvc.perform(post("/api/recurring-transactions/{id}/generate", futureId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Recurring transaction is not due yet"));

        mockMvc.perform(post("/api/recurring-transactions/{id}/generate", endedId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Recurring transaction has ended"));
    }

    @Test
    void createAndUpdateRejectReversedDateRange() throws Exception {
        Long categoryId = createCategory(userAToken, "Food", "EXPENSE");
        Long recurringId = createRecurring(userAToken, categoryId, "EXPENSE", "MONTHLY", "2026-06-01", true);

        mockMvc.perform(post("/api/recurring-transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recurringJsonWithEndDate(
                                categoryId,
                                "EXPENSE",
                                "MONTHLY",
                                "2026-06-30",
                                "2026-06-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("startDate must be on or before endDate"));

        mockMvc.perform(put("/api/recurring-transactions/{id}", recurringId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recurringJsonWithEndDate(
                                categoryId,
                                "EXPENSE",
                                "MONTHLY",
                                "2026-06-30",
                                "2026-06-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("startDate must be on or before endDate"));
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

    private Long createRecurring(
            String token,
            Long categoryId,
            String type,
            String frequency,
            String startDate,
            boolean active) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/recurring-transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recurringJson(categoryId, type, frequency, startDate, active)))
                .andExpect(status().isCreated())
                .andReturn();

        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private Long createRecurringWithEndDate(String token, Long categoryId, String startDate, String endDate) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/recurring-transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "EXPENSE",
                                  "amount": 25.50,
                                  "categoryId": %d,
                                  "description": "Recurring lunch",
                                  "frequency": "DAILY",
                                  "startDate": "%s",
                                  "endDate": "%s"
                                }
                                """.formatted(categoryId, startDate, endDate)))
                .andExpect(status().isCreated())
                .andReturn();

        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private String recurringJson(Long categoryId, String type, String frequency, String startDate, boolean active) {
        return """
                {
                  "type": "%s",
                  "amount": 25.50,
                  "categoryId": %d,
                  "description": "Recurring lunch",
                  "frequency": "%s",
                  "startDate": "%s",
                  "active": %s
                }
                """.formatted(type, categoryId, frequency, startDate, active);
    }

    private String recurringJsonWithEndDate(
            Long categoryId,
            String type,
            String frequency,
            String startDate,
            String endDate) {
        return """
                {
                  "type": "%s",
                  "amount": 25.50,
                  "categoryId": %d,
                  "description": "Recurring lunch",
                  "frequency": "%s",
                  "startDate": "%s",
                  "endDate": "%s"
                }
                """.formatted(type, categoryId, frequency, startDate, endDate);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

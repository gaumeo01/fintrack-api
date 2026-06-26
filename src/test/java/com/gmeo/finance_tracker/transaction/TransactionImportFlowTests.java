package com.gmeo.finance_tracker.transaction;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gmeo.finance_tracker.auth.JwtService;
import com.gmeo.finance_tracker.budget.BudgetRepository;
import com.gmeo.finance_tracker.category.CategoryRepository;
import com.gmeo.finance_tracker.recurring.RecurringTransactionRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionImportFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

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
        transactionRepository.deleteAll();
        recurringTransactionRepository.deleteAll();
        budgetRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        userAToken = createUserAndToken("import-a@example.com", "Import A");
        userBToken = createUserAndToken("import-b@example.com", "Import B");
    }

    @Test
    void importsValidCsvSuccessfully() throws Exception {
        Long categoryId = createCategory(userAToken, "Food", "EXPENSE");
        String csv = """
                type,amount,categoryId,description,transactionDate
                EXPENSE,25.50,%d,"Lunch, with team",2026-06-01
                """.formatted(categoryId);

        mockMvc.perform(multipart("/api/transactions/import")
                        .file(csvFile(csv))
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(1))
                .andExpect(jsonPath("$.successfulRows").value(1))
                .andExpect(jsonPath("$.failedRows").value(0));

        org.assertj.core.api.Assertions.assertThat(transactionRepository.findAll()).hasSize(1);
    }

    @Test
    void importsValidRowsAndReportsInvalidRows() throws Exception {
        Long categoryId = createCategory(userAToken, "Food", "EXPENSE");
        String csv = """
                type,amount,categoryId,description,transactionDate
                EXPENSE,25.50,%d,Lunch,2026-06-01
                EXPENSE,0,%d,Invalid,2026-06-02
                """.formatted(categoryId, categoryId);

        mockMvc.perform(multipart("/api/transactions/import")
                        .file(csvFile(csv))
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(2))
                .andExpect(jsonPath("$.successfulRows").value(1))
                .andExpect(jsonPath("$.failedRows").value(1))
                .andExpect(jsonPath("$.errors[0].rowNumber").value(3));
    }

    @Test
    void rejectsEmptyFileAndMissingHeaders() throws Exception {
        mockMvc.perform(multipart("/api/transactions/import")
                        .file(csvFile(""))
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("CSV file must not be empty"));

        mockMvc.perform(multipart("/api/transactions/import")
                        .file(csvFile("type,amount\nEXPENSE,25.50\n"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("CSV header must be: type,amount,categoryId,description,transactionDate"));
    }

    @Test
    void reportsAnotherUsersCategoryAndMismatchedCategoryTypeAsRowErrors() throws Exception {
        Long userBCategoryId = createCategory(userBToken, "User B Food", "EXPENSE");
        Long incomeCategoryId = createCategory(userAToken, "Salary", "INCOME");
        String csv = """
                type,amount,categoryId,description,transactionDate
                EXPENSE,25.50,%d,Other user,2026-06-01
                EXPENSE,25.50,%d,Mismatch,2026-06-02
                """.formatted(userBCategoryId, incomeCategoryId);

        mockMvc.perform(multipart("/api/transactions/import")
                        .file(csvFile(csv))
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successfulRows").value(0))
                .andExpect(jsonPath("$.failedRows").value(2))
                .andExpect(jsonPath("$.errors[0].message").value("Category not found with id: " + userBCategoryId))
                .andExpect(jsonPath("$.errors[1].message").value("Transaction type must match category type"));
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

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile("file", "transactions.csv", "text/csv", content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

package com.gmeo.finance_tracker.database;

import com.gmeo.finance_tracker.recurring.*;
import com.gmeo.finance_tracker.recurring.dto.*;
import com.gmeo.finance_tracker.recurring.enums.RecurringTransactionFrequency;
import com.gmeo.finance_tracker.transaction.*;
import com.gmeo.finance_tracker.transaction.enums.TransactionType;
import com.gmeo.finance_tracker.category.*;
import com.gmeo.finance_tracker.category.enums.CategoryType;
import com.gmeo.finance_tracker.security.CurrentUserService;
import com.gmeo.finance_tracker.user.*;
import com.gmeo.finance_tracker.user.enums.UserRole;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "FINTRACK_TEST_DB_URL", matches = ".*/finance_tracker_it")
class PostgresIntegrationTests {
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> System.getenv("FINTRACK_TEST_DB_URL"));
        r.add("spring.datasource.username", () -> "fintrack_test");
        r.add("spring.datasource.password", () -> System.getenv("FINTRACK_TEST_DB_PASSWORD"));
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        r.add("spring.flyway.enabled", () -> "true");
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @Autowired UserRepository users;
    @Autowired CategoryRepository categories;
    @Autowired RecurringTransactionRepository recurring;
    @Autowired TransactionRepository transactions;
    @Autowired RecurringTransactionService service;
    @Autowired TransactionService transactionService;
    @MockitoBean CurrentUserService current;
    User user;
    Category category;
    LocalDate date = LocalDate.of(2024, 2, 29);
    @BeforeEach void setup() {
        // This class is gated to a disposable database with an explicit test-only name.
        jdbc.execute("TRUNCATE transactions, recurring_transactions, budgets, accounts, categories, users RESTART IDENTITY CASCADE");
        user = new User(); user.setEmail("test@example.com"); user.setFullName("Test");
        user.setPasswordHash("unused-test-hash"); user.setRole(UserRole.USER); user = users.save(user);
        when(current.getCurrentUser()).thenReturn(user);
        category = new Category(); category.setName("Food"); category.setType(CategoryType.EXPENSE);
        category.setUser(user); category = categories.save(category);
    }
    RecurringTransactionRequest request() {
        var r = new RecurringTransactionRequest(); r.setType(TransactionType.EXPENSE); r.setAmount(new BigDecimal("12.34"));
        r.setCategoryId(category.getId()); r.setStartDate(date); r.setFrequency(RecurringTransactionFrequency.YEARLY);
        return r;
    }
    @Test void migrationsAndExactMoney() {
        flyway.validate(); assertThat(flyway.info().pending()).isEmpty();
        var r = request(); r.setAmount(new BigDecimal("99999999999999999.99"));
        var template = service.createRecurringTransaction(r);
        var result = service.generateTransaction(template.getId(), date);
        assertThat(transactions.findById(result.getTransaction().getId()).orElseThrow().getAmount())
            .isEqualByComparingTo("99999999999999999.99");
        assertThat(result.getNextRunDate()).isEqualTo(LocalDate.of(2025, 2, 28));
    }
    @Test void concurrentOccurrenceAndRetryProduceOneTransaction() throws Exception {
        long id = service.createRecurringTransaction(request()).getId();
        var gate = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Long> task = () -> { gate.await(); return service.generateTransaction(id, date).getTransaction().getId(); };
            var a = pool.submit(task); var b = pool.submit(task); gate.countDown();
            assertThat(a.get(20, TimeUnit.SECONDS)).isEqualTo(b.get(20, TimeUnit.SECONDS));
        }
        service.generateTransaction(id, date);
        assertThat(transactions.count()).isEqualTo(1);
        assertThat(recurring.findById(id).orElseThrow().getLastGeneratedDate()).isEqualTo(date);
    }
    @Test void databaseUniquenessCannotBeBypassed() {
        long id = service.createRecurringTransaction(request()).getId(); service.generateTransaction(id, date);
        assertThatThrownBy(() -> jdbc.update("""
            INSERT INTO transactions(user_id, category_id, type, amount, transaction_date, created_at, updated_at, recurring_id, occurrence_date)
            VALUES (?, ?, 'EXPENSE', 1, ?, now(), now(), ?, ?)
            """, user.getId(), category.getId(), date, id, date)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    @Test void failureAfterInsertRollsBackTransactionAndCursor() {
        long id = service.createRecurringTransaction(request()).getId();
        jdbc.execute("CREATE FUNCTION fail_cursor() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'injected failure'; END $$");
        jdbc.execute("CREATE TRIGGER fail_cursor BEFORE UPDATE ON recurring_transactions FOR EACH ROW EXECUTE FUNCTION fail_cursor()");
        try {
            assertThatThrownBy(() -> service.generateTransaction(id, date)).isInstanceOf(RuntimeException.class);
            assertThat(transactions.count()).isZero();
            assertThat(recurring.findById(id).orElseThrow().getNextRunDate()).isEqualTo(date);
        } finally {
            jdbc.execute("DROP TRIGGER fail_cursor ON recurring_transactions"); jdbc.execute("DROP FUNCTION fail_cursor()");
        }
        service.generateTransaction(id, date); assertThat(transactions.count()).isEqualTo(1);
    }
    @Test void reschedulingPreservesHistoryAndCannotRewind() {
        long id = service.createRecurringTransaction(request()).getId(); service.generateTransaction(id, date);
        var r = request(); r.setFrequency(RecurringTransactionFrequency.MONTHLY);
        assertThatThrownBy(() -> service.updateRecurringTransaction(id, r)).hasMessageContaining("after the last generated");
        r.setStartDate(date.plusDays(1)); r.setAmount(new BigDecimal("99.99"));
        service.updateRecurringTransaction(id, r);
        assertThat(transactions.findAll().getFirst().getAmount()).isEqualByComparingTo("12.34");
        assertThat(recurring.findById(id).orElseThrow().getNextRunDate()).isEqualTo(date.plusDays(1));
    }
    @Test void csvRetriesAndConcurrentUploadsDoNotDuplicate() throws Exception {
        String csv = "type,amount,categoryId,description,transactionDate\nEXPENSE,12.34," + category.getId() + ",Lunch,2024-02-29\n";
        var file = new MockMultipartFile("file", "test.csv", "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        try (var pool = Executors.newFixedThreadPool(2)) {
            var gate = new CountDownLatch(1);
            Callable<Integer> task = () -> { gate.await(); return transactionService.importTransactions(file).getSuccessfulRows(); };
            var a = pool.submit(task); var b = pool.submit(task); gate.countDown();
            assertThat(a.get(20, TimeUnit.SECONDS)).isEqualTo(1); assertThat(b.get(20, TimeUnit.SECONDS)).isEqualTo(1);
        }
        assertThat(transactionService.importTransactions(file).getSuccessfulRows()).isEqualTo(1);
        assertThat(transactions.count()).isEqualTo(1);
    }
    @Test void legacyMonthBudgetUpgradePreservesData() throws Exception {
        jdbc.execute("CREATE SCHEMA legacy_upgrade");
        try {
            // Reproduce an unmanaged historical schema: no range columns or Flyway history.
            String baseline = new String(getClass().getResourceAsStream("/db/migration/V1__baseline.sql").readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            String legacy = baseline.replace("amount numeric(19,2) NOT NULL, start_date date NOT NULL, end_date date NOT NULL,",
                    "amount numeric(19,2) NOT NULL, budget_month date NOT NULL,");
            try (var connection = jdbc.getDataSource().getConnection()) {
                connection.setSchema("legacy_upgrade");
                new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
                        new org.springframework.core.io.ByteArrayResource(legacy.getBytes(java.nio.charset.StandardCharsets.UTF_8))).populate(connection);
            }
            var old = Flyway.configure().dataSource(System.getenv("FINTRACK_TEST_DB_URL"), "fintrack_test", System.getenv("FINTRACK_TEST_DB_PASSWORD"))
                .schemas("legacy_upgrade").defaultSchema("legacy_upgrade").baselineVersion("1").load(); old.baseline();
            jdbc.execute("INSERT INTO legacy_upgrade.users VALUES (1,'legacy@example.com','hash','Legacy','USER',now(),now())");
            jdbc.execute("INSERT INTO legacy_upgrade.categories VALUES (1,'Food','EXPENSE',1,now(),now())");
            jdbc.execute("INSERT INTO legacy_upgrade.budgets(id,user_id,category_id,amount,budget_month,created_at,updated_at) VALUES (1,1,1,100,'2024-02-01',now(),now())");
            var upgrade = Flyway.configure().dataSource(System.getenv("FINTRACK_TEST_DB_URL"), "fintrack_test", System.getenv("FINTRACK_TEST_DB_PASSWORD"))
                .schemas("legacy_upgrade").defaultSchema("legacy_upgrade").load(); upgrade.migrate(); upgrade.validate();
            assertThat(jdbc.queryForObject("SELECT end_date FROM legacy_upgrade.budgets", LocalDate.class)).isEqualTo(date);
            assertThat(jdbc.queryForObject("SELECT budget_month FROM legacy_upgrade.budgets", LocalDate.class)).isEqualTo(date.withDayOfMonth(1));
        } finally { jdbc.execute("DROP SCHEMA legacy_upgrade CASCADE"); }
    }
}

package com.gmeo.finance_tracker.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gmeo.finance_tracker.category.Category;
import com.gmeo.finance_tracker.category.CategoryRepository;
import com.gmeo.finance_tracker.category.enums.CategoryType;
import com.gmeo.finance_tracker.common.dto.PageResponse;
import com.gmeo.finance_tracker.common.exception.BadRequestException;
import com.gmeo.finance_tracker.common.exception.ResourceNotFoundException;
import com.gmeo.finance_tracker.security.CurrentUserService;
import com.gmeo.finance_tracker.transaction.dto.TransactionRequest;
import com.gmeo.finance_tracker.transaction.dto.TransactionResponse;
import com.gmeo.finance_tracker.transaction.enums.TransactionType;
import com.gmeo.finance_tracker.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

class TransactionServiceTests {

    private TransactionRepository transactionRepository;
    private CategoryRepository categoryRepository;
    private CurrentUserService currentUserService;
    private TransactionService transactionService;
    private User currentUser;

    @BeforeEach
    void setUp() {
        transactionRepository = Mockito.mock(TransactionRepository.class);
        categoryRepository = Mockito.mock(CategoryRepository.class);
        currentUserService = Mockito.mock(CurrentUserService.class);
        transactionService = new TransactionService(transactionRepository, categoryRepository, currentUserService);
        currentUser = new User();
        currentUser.setId(7L);
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
    }

    @Test
    void createTransactionUsesCategoryFromCategoryId() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Food");
        category.setType(CategoryType.EXPENSE);

        Transaction savedTransaction = new Transaction();
        savedTransaction.setId(10L);
        savedTransaction.setType(TransactionType.EXPENSE);
        savedTransaction.setAmount(new BigDecimal("25.50"));
        savedTransaction.setCategory(category);
        savedTransaction.setDescription("Lunch");
        savedTransaction.setTransactionDate(LocalDate.of(2026, 5, 20));
        savedTransaction.setCreatedAt(LocalDateTime.of(2026, 5, 20, 10, 0));
        savedTransaction.setUpdatedAt(LocalDateTime.of(2026, 5, 20, 10, 0));

        when(categoryRepository.findByIdAndUserId(1L, 7L)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        TransactionResponse response = transactionService.createTransaction(createRequest());

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getCategory()).isEqualTo(category);
        assertThat(transactionCaptor.getValue().getUser()).isEqualTo(currentUser);
        assertThat(response.getCategoryId()).isEqualTo(1L);
        assertThat(response.getCategoryName()).isEqualTo("Food");
        assertThat(response.getCategoryType()).isEqualTo(CategoryType.EXPENSE);
    }

    @Test
    void createTransactionThrowsResourceNotFoundExceptionWhenCategoryDoesNotExist() {
        when(categoryRepository.findByIdAndUserId(1L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(createRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found with id: 1");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransactionRejectsMismatchedCategoryType() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Salary");
        category.setType(CategoryType.INCOME);

        when(categoryRepository.findByIdAndUserId(1L, 7L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> transactionService.createTransaction(createRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Transaction type must match category type");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void getTransactionsReturnsPaginatedResponse() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Food");
        category.setType(CategoryType.EXPENSE);

        Transaction transaction = new Transaction();
        transaction.setId(10L);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setAmount(new BigDecimal("25.50"));
        transaction.setCategory(category);
        transaction.setDescription("Lunch");
        transaction.setTransactionDate(LocalDate.of(2026, 5, 20));
        transaction.setCreatedAt(LocalDateTime.of(2026, 5, 20, 10, 0));
        transaction.setUpdatedAt(LocalDateTime.of(2026, 5, 20, 10, 0));

        Pageable pageable = PageRequest.of(0, 10);
        when(transactionRepository.findAll(Mockito.<Specification<Transaction>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(transaction), pageable, 1));

        PageResponse<TransactionResponse> response = transactionService.getTransactions(
                TransactionType.EXPENSE,
                1L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                new BigDecimal("10"),
                new BigDecimal("100"),
                "lunch",
                pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getCategoryName()).isEqualTo("Food");
        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.isLast()).isTrue();
    }

    @Test
    void getAllTransactionsListsOnlyCurrentUsersTransactions() {
        Transaction transaction = transaction(10L);
        when(transactionRepository.findAllByUserId(7L)).thenReturn(List.of(transaction));

        List<TransactionResponse> response = transactionService.getAllTransactions();

        verify(transactionRepository).findAllByUserId(7L);
        assertThat(response).extracting(TransactionResponse::getId).containsExactly(10L);
    }

    @Test
    void exportTransactionsReturnsCsvWithEscapedValues() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Food, \"Groceries\"");
        category.setType(CategoryType.EXPENSE);

        Transaction transaction = new Transaction();
        transaction.setId(10L);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setAmount(new BigDecimal("25.50"));
        transaction.setCategory(category);
        transaction.setDescription("Lunch,\nwith \"team\"");
        transaction.setTransactionDate(LocalDate.of(2026, 5, 20));
        transaction.setCreatedAt(LocalDateTime.of(2026, 5, 20, 10, 0));
        transaction.setUpdatedAt(LocalDateTime.of(2026, 5, 20, 11, 30));

        when(transactionRepository.findAll(Mockito.<Specification<Transaction>>any(), any(Sort.class)))
                .thenReturn(List.of(transaction));

        String csv = transactionService.exportTransactions(
                TransactionType.EXPENSE,
                1L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                new BigDecimal("10"),
                new BigDecimal("100"),
                "lunch");

        assertThat(csv).isEqualTo(
                "id,type,amount,categoryId,categoryName,description,transactionDate,createdAt,updatedAt\n"
                        + "10,EXPENSE,25.50,1,\"Food, \"\"Groceries\"\"\",\"Lunch,\n"
                        + "with \"\"team\"\"\",2026-05-20,2026-05-20T10:00,2026-05-20T11:30");

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(transactionRepository).findAll(Mockito.<Specification<Transaction>>any(), sortCaptor.capture());
        assertThat(sortCaptor.getValue().getOrderFor("transactionDate").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(sortCaptor.getValue().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void exportTransactionsReturnsOnlyHeaderWhenNoTransactionsMatch() {
        when(transactionRepository.findAll(Mockito.<Specification<Transaction>>any(), any(Sort.class)))
                .thenReturn(List.of());

        String csv = transactionService.exportTransactions(null, null, null, null, null, null, null);

        assertThat(csv).isEqualTo(
                "id,type,amount,categoryId,categoryName,description,transactionDate,createdAt,updatedAt");
    }

    @Test
    void cannotAccessAnotherUsersTransaction() {
        when(transactionRepository.findByIdAndUserId(10L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransactionById(10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Transaction not found with id: 10");
    }

    @Test
    void cannotUpdateAnotherUsersTransaction() {
        when(transactionRepository.findByIdAndUserId(10L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.updateTransaction(10L, createRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Transaction not found with id: 10");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void updateTransactionRejectsMismatchedCategoryType() {
        Transaction existingTransaction = transaction(10L);
        Category incomeCategory = new Category();
        incomeCategory.setId(2L);
        incomeCategory.setName("Salary");
        incomeCategory.setType(CategoryType.INCOME);

        TransactionRequest request = createRequest();
        request.setCategoryId(2L);

        when(transactionRepository.findByIdAndUserId(10L, 7L)).thenReturn(Optional.of(existingTransaction));
        when(categoryRepository.findByIdAndUserId(2L, 7L)).thenReturn(Optional.of(incomeCategory));

        assertThatThrownBy(() -> transactionService.updateTransaction(10L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Transaction type must match category type");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void getTransactionsRejectsFromDateAfterToDate() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> transactionService.getTransactions(
                null,
                null,
                LocalDate.of(2026, 5, 31),
                LocalDate.of(2026, 5, 1),
                null,
                null,
                null,
                pageable))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("fromDate must be before or equal to toDate");

        verify(transactionRepository, never()).findAll(Mockito.<Specification<Transaction>>any(), any(Pageable.class));
    }

    @Test
    void exportTransactionsRejectsMinAmountGreaterThanMaxAmount() {
        assertThatThrownBy(() -> transactionService.exportTransactions(
                null,
                null,
                null,
                null,
                new BigDecimal("100"),
                new BigDecimal("10"),
                null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("minAmount must be less than or equal to maxAmount");

        verify(transactionRepository, never()).findAll(Mockito.<Specification<Transaction>>any(), any(Sort.class));
    }

    @Test
    void cannotDeleteAnotherUsersTransaction() {
        when(transactionRepository.findByIdAndUserId(10L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.deleteTransaction(10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Transaction not found with id: 10");

        verify(transactionRepository, never()).delete(any(Transaction.class));
    }

    private Transaction transaction(Long id) {
        Category category = new Category();
        category.setId(1L);
        category.setName("Food");
        category.setType(CategoryType.EXPENSE);

        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setAmount(new BigDecimal("25.50"));
        transaction.setCategory(category);
        transaction.setDescription("Lunch");
        transaction.setTransactionDate(LocalDate.of(2026, 5, 20));
        transaction.setCreatedAt(LocalDateTime.of(2026, 5, 20, 10, 0));
        transaction.setUpdatedAt(LocalDateTime.of(2026, 5, 20, 10, 0));
        return transaction;
    }

    private TransactionRequest createRequest() {
        TransactionRequest request = new TransactionRequest();
        request.setType(TransactionType.EXPENSE);
        request.setAmount(new BigDecimal("25.50"));
        request.setCategoryId(1L);
        request.setDescription("Lunch");
        request.setTransactionDate(LocalDate.of(2026, 5, 20));
        return request;
    }
}

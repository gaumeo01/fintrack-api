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
import com.gmeo.finance_tracker.common.exception.ResourceNotFoundException;
import com.gmeo.finance_tracker.transaction.dto.TransactionRequest;
import com.gmeo.finance_tracker.transaction.dto.TransactionResponse;
import com.gmeo.finance_tracker.transaction.enums.TransactionType;
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
import org.springframework.data.jpa.domain.Specification;

class TransactionServiceTests {

    private TransactionRepository transactionRepository;
    private CategoryRepository categoryRepository;
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionRepository = Mockito.mock(TransactionRepository.class);
        categoryRepository = Mockito.mock(CategoryRepository.class);
        transactionService = new TransactionService(transactionRepository, categoryRepository);
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

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        TransactionResponse response = transactionService.createTransaction(createRequest());

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getCategory()).isEqualTo(category);
        assertThat(response.getCategoryId()).isEqualTo(1L);
        assertThat(response.getCategoryName()).isEqualTo("Food");
        assertThat(response.getCategoryType()).isEqualTo(CategoryType.EXPENSE);
    }

    @Test
    void createTransactionThrowsResourceNotFoundExceptionWhenCategoryDoesNotExist() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(createRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found with id: 1");

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
                pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getCategoryName()).isEqualTo("Food");
        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.isLast()).isTrue();
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

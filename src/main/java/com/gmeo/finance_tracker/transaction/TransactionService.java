package com.gmeo.finance_tracker.transaction;

import com.gmeo.finance_tracker.category.Category;
import com.gmeo.finance_tracker.category.CategoryRepository;
import com.gmeo.finance_tracker.common.dto.PageResponse;
import com.gmeo.finance_tracker.common.exception.ResourceNotFoundException;
import com.gmeo.finance_tracker.security.CurrentUserService;
import com.gmeo.finance_tracker.transaction.dto.TransactionRequest;
import com.gmeo.finance_tracker.transaction.dto.TransactionResponse;
import com.gmeo.finance_tracker.transaction.enums.TransactionType;
import com.gmeo.finance_tracker.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;

    public TransactionService(
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            CurrentUserService currentUserService) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.currentUserService = currentUserService;
    }

    public TransactionResponse createTransaction(TransactionRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Category category = findOwnedCategory(request.getCategoryId(), currentUser.getId());

        Transaction transaction = new Transaction();
        transaction.setType(request.getType());
        transaction.setAmount(request.getAmount());
        transaction.setCategory(category);
        transaction.setUser(currentUser);
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate());

        Transaction savedTransaction = transactionRepository.save(transaction);
        return mapToResponse(savedTransaction);
    }

    public List<TransactionResponse> getAllTransactions() {
        User currentUser = currentUserService.getCurrentUser();
        return transactionRepository.findAllByUserId(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public PageResponse<TransactionResponse> getTransactions(
            TransactionType type,
            Long categoryId,
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            Pageable pageable) {
        User currentUser = currentUserService.getCurrentUser();
        Specification<Transaction> specification = TransactionSpecification.withFilters(
                currentUser.getId(),
                type,
                categoryId,
                fromDate,
                toDate,
                minAmount,
                maxAmount);

        Page<TransactionResponse> transactionPage = transactionRepository.findAll(specification, pageable)
                .map(this::mapToResponse);

        return PageResponse.fromPage(transactionPage);
    }

    public TransactionResponse getTransactionById(Long id) {
        Transaction transaction = findOwnedTransaction(id);

        return mapToResponse(transaction);
    }

    public TransactionResponse updateTransaction(Long id, TransactionRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Transaction transaction = findOwnedTransaction(id, currentUser.getId());
        Category category = findOwnedCategory(request.getCategoryId(), currentUser.getId());

        transaction.setType(request.getType());
        transaction.setAmount(request.getAmount());
        transaction.setCategory(category);
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate());

        Transaction savedTransaction = transactionRepository.save(transaction);
        return mapToResponse(savedTransaction);
    }

    public void deleteTransaction(Long id) {
        Transaction transaction = findOwnedTransaction(id);

        transactionRepository.delete(transaction);
    }

    private Category findOwnedCategory(Long categoryId, Long userId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
    }

    private Transaction findOwnedTransaction(Long id) {
        return findOwnedTransaction(id, currentUserService.getCurrentUser().getId());
    }

    private Transaction findOwnedTransaction(Long id, Long userId) {
        return transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setType(transaction.getType());
        response.setAmount(transaction.getAmount());
        response.setCategoryId(transaction.getCategory().getId());
        response.setCategoryName(transaction.getCategory().getName());
        response.setCategoryType(transaction.getCategory().getType());
        response.setDescription(transaction.getDescription());
        response.setTransactionDate(transaction.getTransactionDate());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setUpdatedAt(transaction.getUpdatedAt());
        return response;
    }
}

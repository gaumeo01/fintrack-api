package com.gmeo.finance_tracker.recurring;

import com.gmeo.finance_tracker.category.Category;
import com.gmeo.finance_tracker.category.CategoryRepository;
import com.gmeo.finance_tracker.common.exception.BadRequestException;
import com.gmeo.finance_tracker.common.exception.ResourceNotFoundException;
import com.gmeo.finance_tracker.recurring.dto.RecurringTransactionGenerateResponse;
import com.gmeo.finance_tracker.recurring.dto.RecurringTransactionRequest;
import com.gmeo.finance_tracker.recurring.dto.RecurringTransactionResponse;
import com.gmeo.finance_tracker.recurring.enums.RecurringTransactionFrequency;
import com.gmeo.finance_tracker.security.CurrentUserService;
import com.gmeo.finance_tracker.transaction.Transaction;
import com.gmeo.finance_tracker.transaction.TransactionRepository;
import com.gmeo.finance_tracker.transaction.dto.TransactionResponse;
import com.gmeo.finance_tracker.user.User;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;

    public RecurringTransactionService(
            RecurringTransactionRepository recurringTransactionRepository,
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            CurrentUserService currentUserService) {
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.currentUserService = currentUserService;
    }

    public RecurringTransactionResponse createRecurringTransaction(RecurringTransactionRequest request) {
        validateDateRange(request);
        User currentUser = currentUserService.getCurrentUser();
        Category category = findOwnedCategory(request.getCategoryId(), currentUser.getId());
        validateCategoryType(category, request);

        RecurringTransaction recurringTransaction = new RecurringTransaction();
        recurringTransaction.setUser(currentUser);
        recurringTransaction.setNextRunDate(request.getStartDate());
        recurringTransaction.setActive(request.getActive() == null || request.getActive());
        applyRequest(recurringTransaction, request, category);

        return mapToResponse(recurringTransactionRepository.save(recurringTransaction));
    }

    public List<RecurringTransactionResponse> getRecurringTransactions() {
        User currentUser = currentUserService.getCurrentUser();
        return recurringTransactionRepository.findAllByUserId(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public RecurringTransactionResponse getRecurringTransaction(Long id) {
        return mapToResponse(findOwnedRecurringTransaction(id));
    }

    public RecurringTransactionResponse updateRecurringTransaction(Long id, RecurringTransactionRequest request) {
        validateDateRange(request);
        User currentUser = currentUserService.getCurrentUser();
        RecurringTransaction recurringTransaction = findOwnedRecurringTransaction(id, currentUser.getId());
        Category category = findOwnedCategory(request.getCategoryId(), currentUser.getId());
        validateCategoryType(category, request);

        applyRequest(recurringTransaction, request, category);
        if (request.getActive() != null) {
            recurringTransaction.setActive(request.getActive());
        }

        return mapToResponse(recurringTransactionRepository.save(recurringTransaction));
    }

    public void deleteRecurringTransaction(Long id) {
        recurringTransactionRepository.delete(findOwnedRecurringTransaction(id));
    }

    public RecurringTransactionGenerateResponse generateTransaction(Long id) {
        RecurringTransaction recurringTransaction = findOwnedRecurringTransaction(id);

        if (!recurringTransaction.isActive()) {
            throw new BadRequestException("Recurring transaction is inactive");
        }

        LocalDate today = LocalDate.now();
        if (recurringTransaction.getNextRunDate().isAfter(today)) {
            throw new BadRequestException("Recurring transaction is not due yet");
        }

        if (recurringTransaction.getEndDate() != null
                && recurringTransaction.getNextRunDate().isAfter(recurringTransaction.getEndDate())) {
            throw new BadRequestException("Recurring transaction has ended");
        }

        Transaction transaction = new Transaction();
        transaction.setUser(recurringTransaction.getUser());
        transaction.setType(recurringTransaction.getType());
        transaction.setAmount(recurringTransaction.getAmount());
        transaction.setCategory(recurringTransaction.getCategory());
        transaction.setDescription(recurringTransaction.getDescription());
        transaction.setTransactionDate(recurringTransaction.getNextRunDate());

        Transaction savedTransaction = transactionRepository.save(transaction);
        recurringTransaction.setNextRunDate(nextDate(
                recurringTransaction.getNextRunDate(),
                recurringTransaction.getFrequency()));
        RecurringTransaction savedRecurringTransaction = recurringTransactionRepository.save(recurringTransaction);

        return new RecurringTransactionGenerateResponse(
                mapTransactionToResponse(savedTransaction),
                savedRecurringTransaction.getNextRunDate());
    }

    private void applyRequest(
            RecurringTransaction recurringTransaction,
            RecurringTransactionRequest request,
            Category category) {
        recurringTransaction.setType(request.getType());
        recurringTransaction.setAmount(request.getAmount());
        recurringTransaction.setCategory(category);
        recurringTransaction.setDescription(request.getDescription());
        recurringTransaction.setFrequency(request.getFrequency());
        recurringTransaction.setStartDate(request.getStartDate());
        recurringTransaction.setEndDate(request.getEndDate());
    }

    private Category findOwnedCategory(Long categoryId, Long userId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
    }

    private RecurringTransaction findOwnedRecurringTransaction(Long id) {
        return findOwnedRecurringTransaction(id, currentUserService.getCurrentUser().getId());
    }

    private RecurringTransaction findOwnedRecurringTransaction(Long id, Long userId) {
        return recurringTransactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring transaction not found with id: " + id));
    }

    private void validateCategoryType(Category category, RecurringTransactionRequest request) {
        if (!category.getType().name().equals(request.getType().name())) {
            throw new BadRequestException("Category type must match transaction type");
        }
    }

    private void validateDateRange(RecurringTransactionRequest request) {
        if (request.getStartDate() != null
                && request.getEndDate() != null
                && request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException("startDate must be on or before endDate");
        }
    }

    private LocalDate nextDate(LocalDate currentDate, RecurringTransactionFrequency frequency) {
        return switch (frequency) {
            case DAILY -> currentDate.plusDays(1);
            case WEEKLY -> currentDate.plusWeeks(1);
            case MONTHLY -> currentDate.plusMonths(1);
            case YEARLY -> currentDate.plusYears(1);
        };
    }

    private RecurringTransactionResponse mapToResponse(RecurringTransaction recurringTransaction) {
        RecurringTransactionResponse response = new RecurringTransactionResponse();
        response.setId(recurringTransaction.getId());
        response.setType(recurringTransaction.getType());
        response.setAmount(recurringTransaction.getAmount());
        response.setCategoryId(recurringTransaction.getCategory().getId());
        response.setCategoryName(recurringTransaction.getCategory().getName());
        response.setCategoryType(recurringTransaction.getCategory().getType());
        response.setDescription(recurringTransaction.getDescription());
        response.setFrequency(recurringTransaction.getFrequency());
        response.setStartDate(recurringTransaction.getStartDate());
        response.setEndDate(recurringTransaction.getEndDate());
        response.setNextRunDate(recurringTransaction.getNextRunDate());
        response.setActive(recurringTransaction.isActive());
        response.setCreatedAt(recurringTransaction.getCreatedAt());
        response.setUpdatedAt(recurringTransaction.getUpdatedAt());
        return response;
    }

    private TransactionResponse mapTransactionToResponse(Transaction transaction) {
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

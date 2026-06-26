package com.gmeo.finance_tracker.transaction;

import com.gmeo.finance_tracker.category.Category;
import com.gmeo.finance_tracker.category.CategoryRepository;
import com.gmeo.finance_tracker.common.dto.PageResponse;
import com.gmeo.finance_tracker.common.exception.BadRequestException;
import com.gmeo.finance_tracker.common.exception.ResourceNotFoundException;
import com.gmeo.finance_tracker.security.CurrentUserService;
import com.gmeo.finance_tracker.transaction.dto.TransactionImportError;
import com.gmeo.finance_tracker.transaction.dto.TransactionImportResponse;
import com.gmeo.finance_tracker.transaction.dto.TransactionRequest;
import com.gmeo.finance_tracker.transaction.dto.TransactionResponse;
import com.gmeo.finance_tracker.transaction.enums.TransactionType;
import com.gmeo.finance_tracker.user.User;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TransactionService {

    private static final String CSV_HEADER =
            "id,type,amount,categoryId,categoryName,description,transactionDate,createdAt,updatedAt";
    private static final List<String> IMPORT_HEADERS = List.of(
            "type",
            "amount",
            "categoryId",
            "description",
            "transactionDate");

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
        validateCategoryType(category, request.getType());

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
            String keyword,
            Pageable pageable) {
        validateFilterRanges(fromDate, toDate, minAmount, maxAmount);

        User currentUser = currentUserService.getCurrentUser();
        Specification<Transaction> specification = TransactionSpecification.withFilters(
                currentUser.getId(),
                type,
                categoryId,
                fromDate,
                toDate,
                minAmount,
                maxAmount,
                keyword);

        Page<TransactionResponse> transactionPage = transactionRepository.findAll(specification, pageable)
                .map(this::mapToResponse);

        return PageResponse.fromPage(transactionPage);
    }

    public String exportTransactions(
            TransactionType type,
            Long categoryId,
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String keyword) {
        validateFilterRanges(fromDate, toDate, minAmount, maxAmount);

        User currentUser = currentUserService.getCurrentUser();
        Specification<Transaction> specification = TransactionSpecification.withFilters(
                currentUser.getId(),
                type,
                categoryId,
                fromDate,
                toDate,
                minAmount,
                maxAmount,
                keyword);

        Sort sort = Sort.by(
                Sort.Order.desc("transactionDate"),
                Sort.Order.desc("id"));

        List<Transaction> transactions = transactionRepository.findAll(specification, sort);

        StringBuilder csv = new StringBuilder(CSV_HEADER);
        for (Transaction transaction : transactions) {
            csv.append('\n')
                    .append(csvValue(transaction.getId()))
                    .append(',')
                    .append(csvValue(transaction.getType()))
                    .append(',')
                    .append(csvValue(transaction.getAmount()))
                    .append(',')
                    .append(csvValue(transaction.getCategory().getId()))
                    .append(',')
                    .append(csvValue(transaction.getCategory().getName()))
                    .append(',')
                    .append(csvValue(transaction.getDescription()))
                    .append(',')
                    .append(csvValue(transaction.getTransactionDate()))
                    .append(',')
                    .append(csvValue(transaction.getCreatedAt()))
                    .append(',')
                    .append(csvValue(transaction.getUpdatedAt()));
        }

        return csv.toString();
    }

    public TransactionImportResponse importTransactions(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("CSV file must not be empty");
        }

        List<List<String>> rows = parseCsv(file);
        if (rows.isEmpty() || isBlankRow(rows.get(0))) {
            throw new BadRequestException("CSV file must not be empty");
        }

        validateImportHeaders(rows.get(0));

        User currentUser = currentUserService.getCurrentUser();
        List<TransactionImportError> errors = new ArrayList<>();
        int successfulRows = 0;

        for (int index = 1; index < rows.size(); index++) {
            List<String> row = rows.get(index);
            if (isBlankRow(row)) {
                continue;
            }

            try {
                Transaction transaction = mapImportRow(row, currentUser);
                transactionRepository.save(transaction);
                successfulRows++;
            } catch (IllegalArgumentException | DateTimeParseException | ResourceNotFoundException | BadRequestException exception) {
                errors.add(new TransactionImportError(index + 1, exception.getMessage()));
            }
        }

        int totalRows = successfulRows + errors.size();
        return new TransactionImportResponse(totalRows, successfulRows, errors.size(), errors);
    }

    public TransactionResponse getTransactionById(Long id) {
        Transaction transaction = findOwnedTransaction(id);

        return mapToResponse(transaction);
    }

    public TransactionResponse updateTransaction(Long id, TransactionRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Transaction transaction = findOwnedTransaction(id, currentUser.getId());
        Category category = findOwnedCategory(request.getCategoryId(), currentUser.getId());
        validateCategoryType(category, request.getType());

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

    private void validateCategoryType(Category category, TransactionType type) {
        if (!category.getType().name().equals(type.name())) {
            throw new BadRequestException("Transaction type must match category type");
        }
    }

    private void validateFilterRanges(
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal minAmount,
            BigDecimal maxAmount) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate must be before or equal to toDate");
        }
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new BadRequestException("minAmount must be less than or equal to maxAmount");
        }
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

    private String csvValue(Object value) {
        if (value == null) {
            return "";
        }

        String text = value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }

        return text;
    }

    private List<List<String>> parseCsv(MultipartFile file) {
        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new BadRequestException("Could not read CSV file");
        }

        if (content.isBlank()) {
            return List.of();
        }

        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean inQuotes = false;

        for (int index = 0; index < content.length(); index++) {
            char character = content.charAt(index);
            if (inQuotes) {
                if (character == '"') {
                    if (index + 1 < content.length() && content.charAt(index + 1) == '"') {
                        value.append('"');
                        index++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    value.append(character);
                }
                continue;
            }

            if (character == '"') {
                inQuotes = true;
            } else if (character == ',') {
                row.add(value.toString());
                value.setLength(0);
            } else if (character == '\n') {
                row.add(value.toString());
                rows.add(row);
                row = new ArrayList<>();
                value.setLength(0);
            } else if (character != '\r') {
                value.append(character);
            }
        }

        if (inQuotes) {
            throw new BadRequestException("CSV contains an unclosed quoted value");
        }

        row.add(value.toString());
        if (!isBlankRow(row)) {
            rows.add(row);
        }

        return rows;
    }

    private void validateImportHeaders(List<String> headers) {
        List<String> normalizedHeaders = headers.stream()
                .map(String::trim)
                .toList();
        if (!normalizedHeaders.equals(IMPORT_HEADERS)) {
            throw new BadRequestException("CSV header must be: " + String.join(",", IMPORT_HEADERS));
        }
    }

    private boolean isBlankRow(List<String> row) {
        return row.stream().allMatch(value -> value == null || value.isBlank());
    }

    private Transaction mapImportRow(List<String> row, User currentUser) {
        if (row.size() != IMPORT_HEADERS.size()) {
            throw new BadRequestException("Row must contain " + IMPORT_HEADERS.size() + " columns");
        }

        TransactionType type = TransactionType.valueOf(row.get(0).trim());
        BigDecimal amount = new BigDecimal(row.get(1).trim());
        if (amount.compareTo(new BigDecimal("0.01")) < 0) {
            throw new BadRequestException("Amount must be greater than or equal to 0.01");
        }

        Long categoryId = Long.valueOf(row.get(2).trim());
        Category category = findOwnedCategory(categoryId, currentUser.getId());
        validateCategoryType(category, type);

        Transaction transaction = new Transaction();
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setCategory(category);
        transaction.setUser(currentUser);
        transaction.setDescription(row.get(3));
        transaction.setTransactionDate(LocalDate.parse(row.get(4).trim()));
        return transaction;
    }
}

package com.gmeo.finance_tracker.transaction;

import com.gmeo.finance_tracker.transaction.enums.TransactionType;
import jakarta.persistence.criteria.Expression;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public final class TransactionSpecification {

    private TransactionSpecification() {
    }

    public static Specification<Transaction> withFilters(
            Long userId,
            TransactionType type,
            Long categoryId,
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal minAmount,
            BigDecimal maxAmount) {
        return withFilters(userId, type, categoryId, fromDate, toDate, minAmount, maxAmount, null);
    }

    public static Specification<Transaction> withFilters(
            Long userId,
            TransactionType type,
            Long categoryId,
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String keyword) {
        Specification<Transaction> specification = hasUserId(userId);

        if (type != null) {
            specification = specification.and(hasType(type));
        }

        if (categoryId != null) {
            specification = specification.and(hasCategoryId(categoryId));
        }

        if (fromDate != null) {
            specification = specification.and(transactionDateGreaterThanOrEqualTo(fromDate));
        }

        if (toDate != null) {
            specification = specification.and(transactionDateLessThanOrEqualTo(toDate));
        }

        if (minAmount != null) {
            specification = specification.and(amountGreaterThanOrEqualTo(minAmount));
        }

        if (maxAmount != null) {
            specification = specification.and(amountLessThanOrEqualTo(maxAmount));
        }

        if (keyword != null && !keyword.isBlank()) {
            specification = specification.and(hasKeyword(keyword));
        }

        return specification;
    }

    private static Specification<Transaction> hasUserId(Long userId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("user").get("id"), userId);
    }

    private static Specification<Transaction> hasType(TransactionType type) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("type"), type);
    }

    private static Specification<Transaction> hasCategoryId(Long categoryId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("category").get("id"), categoryId);
    }

    private static Specification<Transaction> transactionDateGreaterThanOrEqualTo(LocalDate fromDate) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("transactionDate"), fromDate);
    }

    private static Specification<Transaction> transactionDateLessThanOrEqualTo(LocalDate toDate) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("transactionDate"), toDate);
    }

    private static Specification<Transaction> amountGreaterThanOrEqualTo(BigDecimal minAmount) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("amount"), minAmount);
    }

    private static Specification<Transaction> amountLessThanOrEqualTo(BigDecimal maxAmount) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("amount"), maxAmount);
    }

    private static Specification<Transaction> hasKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            String pattern = "%" + keyword.toLowerCase() + "%";
            Expression<String> description = criteriaBuilder.lower(root.<String>get("description"));
            Expression<String> categoryName = criteriaBuilder.lower(root.get("category").<String>get("name"));
            return criteriaBuilder.or(
                    criteriaBuilder.like(description, pattern),
                    criteriaBuilder.like(categoryName, pattern));
        };
    }
}

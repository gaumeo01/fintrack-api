package com.gmeo.finance_tracker.transaction;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gmeo.finance_tracker.transaction.enums.TransactionType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

class TransactionSpecificationTests {

    private Root<Transaction> root;
    private CriteriaQuery<?> query;
    private CriteriaBuilder criteriaBuilder;
    private Predicate basePredicate;
    private Predicate filterPredicate;
    private Predicate combinedPredicate;

    @BeforeEach
    void setUp() {
        root = mock(Root.class);
        query = mock(CriteriaQuery.class);
        criteriaBuilder = mock(CriteriaBuilder.class);
        basePredicate = mock(Predicate.class);
        filterPredicate = mock(Predicate.class);
        combinedPredicate = mock(Predicate.class);
        Path<Object> userPath = mock(Path.class);
        Path<Long> userIdPath = mock(Path.class);

        when(root.get("user")).thenReturn(userPath);
        when(userPath.<Long>get("id")).thenReturn(userIdPath);
        when(criteriaBuilder.equal(userIdPath, 7L)).thenReturn(filterPredicate);
        when(criteriaBuilder.conjunction()).thenReturn(basePredicate);
        when(criteriaBuilder.and(any(Predicate.class), any(Predicate.class))).thenReturn(combinedPredicate);
    }

    @Test
    void withFiltersAddsTypeFilterWhenTypeIsProvided() {
        Path<TransactionType> typePath = mock(Path.class);
        when(root.<TransactionType>get("type")).thenReturn(typePath);
        when(criteriaBuilder.equal(typePath, TransactionType.EXPENSE)).thenReturn(filterPredicate);

        Specification<Transaction> specification = TransactionSpecification.withFilters(
                7L,
                TransactionType.EXPENSE,
                null,
                null,
                null,
                null,
                null);

        specification.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).equal(typePath, TransactionType.EXPENSE);
    }

    @Test
    void withFiltersAddsCategoryIdFilterWhenCategoryIdIsProvided() {
        Path<Object> categoryPath = mock(Path.class);
        Path<Long> categoryIdPath = mock(Path.class);
        when(root.get("category")).thenReturn(categoryPath);
        when(categoryPath.<Long>get("id")).thenReturn(categoryIdPath);
        when(criteriaBuilder.equal(categoryIdPath, 1L)).thenReturn(filterPredicate);

        Specification<Transaction> specification = TransactionSpecification.withFilters(
                7L,
                null,
                1L,
                null,
                null,
                null,
                null);

        specification.toPredicate(root, query, criteriaBuilder);

        verify(root).get("category");
        verify(categoryPath).get("id");
        verify(criteriaBuilder).equal(categoryIdPath, 1L);
    }

    @Test
    void withFiltersAddsDateRangeFiltersWhenDatesAreProvided() {
        Path<LocalDate> transactionDatePath = mock(Path.class);
        LocalDate fromDate = LocalDate.of(2026, 5, 1);
        LocalDate toDate = LocalDate.of(2026, 5, 31);
        when(root.<LocalDate>get("transactionDate")).thenReturn(transactionDatePath);
        when(criteriaBuilder.greaterThanOrEqualTo(transactionDatePath, fromDate)).thenReturn(filterPredicate);
        when(criteriaBuilder.lessThanOrEqualTo(transactionDatePath, toDate)).thenReturn(filterPredicate);

        Specification<Transaction> specification = TransactionSpecification.withFilters(
                7L,
                null,
                null,
                fromDate,
                toDate,
                null,
                null);

        specification.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).greaterThanOrEqualTo(transactionDatePath, fromDate);
        verify(criteriaBuilder).lessThanOrEqualTo(transactionDatePath, toDate);
    }

    @Test
    void withFiltersAddsAmountRangeFiltersWhenAmountsAreProvided() {
        Path<BigDecimal> amountPath = mock(Path.class);
        BigDecimal minAmount = new BigDecimal("10.00");
        BigDecimal maxAmount = new BigDecimal("100.00");
        when(root.<BigDecimal>get("amount")).thenReturn(amountPath);
        when(criteriaBuilder.greaterThanOrEqualTo(amountPath, minAmount)).thenReturn(filterPredicate);
        when(criteriaBuilder.lessThanOrEqualTo(amountPath, maxAmount)).thenReturn(filterPredicate);

        Specification<Transaction> specification = TransactionSpecification.withFilters(
                7L,
                null,
                null,
                null,
                null,
                minAmount,
                maxAmount);

        specification.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).greaterThanOrEqualTo(amountPath, minAmount);
        verify(criteriaBuilder).lessThanOrEqualTo(amountPath, maxAmount);
    }

    @Test
    void withFiltersAlwaysAddsUserFilterWhenOtherParametersAreMissing() {
        Specification<Transaction> specification = TransactionSpecification.withFilters(
                7L,
                null,
                null,
                null,
                null,
                null,
                null);

        specification.toPredicate(root, query, criteriaBuilder);

        verify(root).get("user");
    }
}

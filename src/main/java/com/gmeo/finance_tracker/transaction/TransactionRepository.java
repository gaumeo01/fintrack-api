package com.gmeo.finance_tracker.transaction;

import com.gmeo.finance_tracker.transaction.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    List<Transaction> findAllByUserId(Long userId);

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    @Query("""
            select t.category.id as categoryId, coalesce(sum(t.amount), 0) as spentAmount
            from Transaction t
            where t.user.id = :userId
              and t.type = :type
              and t.transactionDate >= :fromDate
              and t.transactionDate < :toDate
              and t.category.id in :categoryIds
            group by t.category.id
            """)
    List<CategorySpendingTotal> sumAmountsByCategory(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("categoryIds") List<Long> categoryIds);

    interface CategorySpendingTotal {

        Long getCategoryId();

        BigDecimal getSpentAmount();
    }
}

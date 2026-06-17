package com.gmeo.finance_tracker.budget;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findAllByUserIdAndMonthOrderByCategoryNameAsc(Long userId, LocalDate month);

    Optional<Budget> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndCategoryIdAndMonth(Long userId, Long categoryId, LocalDate month);

    boolean existsByUserIdAndCategoryIdAndMonthAndIdNot(Long userId, Long categoryId, LocalDate month, Long id);
}

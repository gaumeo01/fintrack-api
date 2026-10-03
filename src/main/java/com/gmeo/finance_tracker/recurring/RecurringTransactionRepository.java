package com.gmeo.finance_tracker.recurring;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from RecurringTransaction r where r.id = :id and r.user.id = :userId")
    Optional<RecurringTransaction> findLockedByIdAndUserId(Long id, Long userId);

    List<RecurringTransaction> findAllByUserId(Long userId);

    Optional<RecurringTransaction> findByIdAndUserId(Long id, Long userId);
}

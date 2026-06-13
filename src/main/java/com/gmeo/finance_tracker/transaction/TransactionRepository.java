package com.gmeo.finance_tracker.transaction;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    List<Transaction> findAllByUserId(Long userId);

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);
}

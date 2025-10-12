package com.minishop.paymentservice.repository;

import com.minishop.paymentservice.entity.PaymentStatus;
import com.minishop.paymentservice.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    Optional<PaymentTransaction> findByOrderId(UUID orderId);

    Optional<PaymentTransaction> findByTransactionCode(String transactionCode);

    List<PaymentTransaction> findByStatusInAndExpiredAtBefore(Collection<PaymentStatus> statuses, Instant timestamp);
}

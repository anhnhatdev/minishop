package com.minishop.paymentservice.repository;

import com.minishop.paymentservice.entity.PaymentCallbackLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentCallbackLogRepository extends JpaRepository<PaymentCallbackLog, UUID> {

    List<PaymentCallbackLog> findByTransactionIdOrderByReceivedAtDesc(UUID transactionId);
}

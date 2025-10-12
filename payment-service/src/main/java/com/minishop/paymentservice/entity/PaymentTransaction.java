package com.minishop.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "transaction_code", nullable = false, unique = true, length = 50)
    private String transactionCode;

    @Column(name = "gateway_transaction_id", length = 100)
    private String gatewayTransactionId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.INITIATED;

    @Column(name = "gateway_response_code", length = 20)
    private String gatewayResponseCode;

    @Column(name = "payment_url", length = 1000)
    private String paymentUrl;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "expired_at", nullable = false)
    private Instant expiredAt;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (expiredAt == null) {
            expiredAt = createdAt.plusSeconds(15 * 60); // 15 minutes default
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

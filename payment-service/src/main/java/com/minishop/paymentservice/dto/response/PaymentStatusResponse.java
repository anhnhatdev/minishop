package com.minishop.paymentservice.dto.response;

import com.minishop.paymentservice.entity.PaymentMethod;
import com.minishop.paymentservice.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentStatusResponse {

    private UUID id;
    private UUID orderId;
    private String transactionCode;
    private String gatewayTransactionId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String paymentUrl;
    private Instant paidAt;
    private Instant expiredAt;
}

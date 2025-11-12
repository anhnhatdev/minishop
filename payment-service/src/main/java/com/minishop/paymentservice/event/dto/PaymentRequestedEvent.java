package com.minishop.paymentservice.event.dto;

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
public class PaymentRequestedEvent {

    private String eventId;
    private String eventType;
    private UUID orderId;
    private UUID userId;
    private BigDecimal amount;
    private String paymentMethod;
    private String clientIp;
    private Instant timestamp;
}

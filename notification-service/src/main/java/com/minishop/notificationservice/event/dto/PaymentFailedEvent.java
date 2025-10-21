package com.minishop.notificationservice.event.dto;

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
public class PaymentFailedEvent {

    private String eventId;
    private String eventType;
    private UUID orderId;
    private String orderCode;
    private UUID userId;
    private String customerName;
    private String customerEmail;
    private BigDecimal totalAmount;
    private String reason;
    private Instant timestamp;
}

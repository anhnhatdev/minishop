package com.minishop.paymentservice.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSucceededEvent {

    private String eventId;
    private String eventType;
    private UUID orderId;
    private String transactionId;
    private Instant timestamp;
}

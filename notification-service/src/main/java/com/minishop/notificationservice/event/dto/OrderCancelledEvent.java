package com.minishop.notificationservice.event.dto;

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
public class OrderCancelledEvent {

    private String eventId;
    private String eventType;
    private UUID orderId;
    private String orderCode;
    private UUID userId;
    private String customerName;
    private String customerEmail;
    private String reason;
    private Instant timestamp;
}

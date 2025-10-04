package com.minishop.orderservice.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedEvent {

    private String eventId;
    private String eventType;
    private UUID orderId;
    private String orderCode;
    private UUID userId;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private List<OrderItemEventDto> items;
    private Instant timestamp;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemEventDto {
        private UUID productId;
        private String productName;
        private BigDecimal price;
        private Integer quantity;
    }
}

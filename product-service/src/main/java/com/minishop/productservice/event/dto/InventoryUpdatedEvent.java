package com.minishop.productservice.event.dto;

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
public class InventoryUpdatedEvent {

    private String eventId;
    private String eventType;
    private UUID productId;
    private Integer availableQuantity;
    private Instant timestamp;
}

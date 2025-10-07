package com.minishop.inventoryservice.dto.response;

import com.minishop.inventoryservice.entity.StockMovementType;
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
public class StockMovementResponse {

    private UUID id;
    private UUID productId;
    private StockMovementType type;
    private Integer quantityChange;
    private UUID referenceOrderId;
    private String note;
    private Instant createdAt;
}

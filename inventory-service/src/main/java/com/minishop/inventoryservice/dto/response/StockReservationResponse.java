package com.minishop.inventoryservice.dto.response;

import com.minishop.inventoryservice.entity.ReservationStatus;
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
public class StockReservationResponse {

    private UUID id;
    private UUID orderId;
    private UUID productId;
    private Integer quantity;
    private ReservationStatus status;
    private Instant reservedAt;
    private Instant expiresAt;
}

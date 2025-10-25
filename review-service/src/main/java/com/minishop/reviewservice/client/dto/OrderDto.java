package com.minishop.reviewservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDto {

    private UUID id;
    private String orderCode;
    private UUID userId;
    private String customerName;
    private String customerEmail;
    private String status;
    private BigDecimal totalAmount;
    @Builder.Default
    private List<OrderItemDto> items = new ArrayList<>();
}

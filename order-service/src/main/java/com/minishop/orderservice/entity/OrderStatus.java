package com.minishop.orderservice.entity;

public enum OrderStatus {
    PENDING,
    STOCK_RESERVED,
    CONFIRMED,
    PROCESSING,
    SHIPPING,
    DELIVERED,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus newStatus) {
        if (this == newStatus) {
            return true;
        }
        return switch (this) {
            case PENDING -> newStatus == STOCK_RESERVED || newStatus == CANCELLED;
            case STOCK_RESERVED -> newStatus == CONFIRMED || newStatus == CANCELLED;
            case CONFIRMED -> newStatus == PROCESSING || newStatus == CANCELLED;
            case PROCESSING -> newStatus == SHIPPING || newStatus == CANCELLED;
            case SHIPPING -> newStatus == DELIVERED;
            case DELIVERED -> newStatus == COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}

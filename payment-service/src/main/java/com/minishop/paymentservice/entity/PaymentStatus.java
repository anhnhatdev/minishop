package com.minishop.paymentservice.entity;

public enum PaymentStatus {
    INITIATED,
    PENDING,
    SUCCESS,
    FAILED,
    EXPIRED,
    REFUNDED;

    public boolean canTransitionTo(PaymentStatus newStatus) {
        if (this == newStatus) return true;
        return switch (this) {
            case INITIATED -> newStatus == PENDING || newStatus == SUCCESS || newStatus == FAILED || newStatus == EXPIRED;
            case PENDING -> newStatus == SUCCESS || newStatus == FAILED || newStatus == EXPIRED;
            case SUCCESS -> newStatus == REFUNDED;
            case FAILED, EXPIRED, REFUNDED -> false;
        };
    }
}

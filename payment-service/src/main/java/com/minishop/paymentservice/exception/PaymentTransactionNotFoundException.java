package com.minishop.paymentservice.exception;

public class PaymentTransactionNotFoundException extends RuntimeException {
    public PaymentTransactionNotFoundException(String message) {
        super(message);
    }
}

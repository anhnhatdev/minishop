package com.minishop.productservice.exception;

public class UnauthorizedProductAccessException extends RuntimeException {
    public UnauthorizedProductAccessException(String message) {
        super(message);
    }
}

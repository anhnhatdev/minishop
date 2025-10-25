package com.minishop.reviewservice.exception;

public class ReviewNotEligibleException extends RuntimeException {
    public ReviewNotEligibleException(String message) {
        super(message);
    }
}

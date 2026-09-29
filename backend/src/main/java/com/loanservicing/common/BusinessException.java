package com.loanservicing.common;

/**
 * Thrown when a business rule is broken, e.g. "payment is less than the amount due".
 * Returned to React as HTTP 422 with the message, so the UI can show it.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}

package com.example.employeemanagement.common.exception;

/**
 * Thrown for EMS business-rule violations that aren't simple bean-validation
 * failures (e.g. checking out before checking in, approving an already-rejected
 * leave). Mapped to HTTP 400 by {@link GlobalExceptionHandler}.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}

package com.example.employeemanagement.common.exception;

/**
 * Thrown when an EMS create/update operation would violate a uniqueness rule
 * (e.g. a department name that already exists). Mapped to HTTP 409 by
 * {@link GlobalExceptionHandler}.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}

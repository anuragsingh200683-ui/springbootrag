package com.example.employeemanagement.common.exception;

/**
 * Thrown when the call from Spring Boot to fastapi-service's AI Assistant
 * endpoints fails or is unreachable. Mapped to HTTP 502 by GlobalExceptionHandler.
 */
public class AiServiceException extends RuntimeException {

    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

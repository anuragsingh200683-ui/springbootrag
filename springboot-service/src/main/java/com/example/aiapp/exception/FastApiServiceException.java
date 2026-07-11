package com.example.aiapp.exception;

public class FastApiServiceException extends RuntimeException {
    public FastApiServiceException(String message) {
        super(message);
    }

    public FastApiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

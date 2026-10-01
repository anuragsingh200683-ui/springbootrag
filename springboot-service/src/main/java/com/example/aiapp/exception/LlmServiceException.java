package com.example.aiapp.exception;

public class LlmServiceException extends RuntimeException {
    public LlmServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.example.aiapp.exception;

/** No document chunks were similar enough to the question to answer it from. */
public class NoRelevantContextException extends RuntimeException {
    public NoRelevantContextException(String message) {
        super(message);
    }
}

package com.nirvaha.nirvaha_backend.exception;

public class InvalidWorkflowException extends RuntimeException {
    public InvalidWorkflowException(String message) {
        super(message);
    }
}

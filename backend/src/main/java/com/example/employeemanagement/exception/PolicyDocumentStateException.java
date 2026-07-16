package com.example.employeemanagement.exception;

/**
 * Thrown for state-machine violations on a policy document
 * (e.g. activating a document that hasn't finished processing).
 */
public class PolicyDocumentStateException extends RuntimeException {

    private final String errorCode;

    public PolicyDocumentStateException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

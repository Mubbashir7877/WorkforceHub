package com.example.employeemanagement.exception;

/**
 * Thrown whenever the AI assistant/knowledge base cannot service a request —
 * AI disabled, no provider configured, a provider call failed, an unusable
 * response was returned, or the knowledge base itself is unavailable.
 */
public class AiUnavailableException extends RuntimeException {

    private final String errorCode;

    public AiUnavailableException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

package com.example.employeemanagement.exception;

/**
 * Thrown when text extraction, chunking, or embedding of a stored
 * policy document fails.
 */
public class DocumentProcessingException extends RuntimeException {

    private final String errorCode;

    public DocumentProcessingException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

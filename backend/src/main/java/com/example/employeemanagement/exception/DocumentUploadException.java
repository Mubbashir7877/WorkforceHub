package com.example.employeemanagement.exception;

/**
 * Thrown when an uploaded policy document fails storage-layer validation
 * (unsupported type, oversized, unsafe filename, etc).
 */
public class DocumentUploadException extends RuntimeException {

    private final String errorCode;

    public DocumentUploadException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

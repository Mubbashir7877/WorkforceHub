package com.example.employeemanagement.exception;

public class PolicyDocumentNotFoundException extends RuntimeException {

    public PolicyDocumentNotFoundException(String message) {
        super(message);
    }
}

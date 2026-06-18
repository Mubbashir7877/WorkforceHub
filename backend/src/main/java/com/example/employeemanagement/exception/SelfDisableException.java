package com.example.employeemanagement.exception;

public class SelfDisableException extends RuntimeException {
    public SelfDisableException(String message) {
        super(message);
    }
}

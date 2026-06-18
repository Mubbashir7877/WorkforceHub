package com.example.employeemanagement.exception;

public class EmployeeAlreadyLinkedException extends RuntimeException {
    public EmployeeAlreadyLinkedException(String message) {
        super(message);
    }
}

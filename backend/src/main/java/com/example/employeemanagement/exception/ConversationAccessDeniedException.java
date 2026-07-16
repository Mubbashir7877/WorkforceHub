package com.example.employeemanagement.exception;

public class ConversationAccessDeniedException extends RuntimeException {

    public ConversationAccessDeniedException(String message) {
        super(message);
    }
}

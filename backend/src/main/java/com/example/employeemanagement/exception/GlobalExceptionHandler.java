package com.example.employeemanagement.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), request, null);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", ex.getMessage(), request, null);
    }

    @ExceptionHandler(EmployeeAlreadyLinkedException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeAlreadyLinked(EmployeeAlreadyLinkedException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "EMPLOYEE_ALREADY_LINKED", ex.getMessage(), request, null);
    }

    @ExceptionHandler(LastAdminRoleRemovalException.class)
    public ResponseEntity<ErrorResponse> handleLastAdminRoleRemoval(LastAdminRoleRemovalException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "LAST_ADMIN_ROLE", ex.getMessage(), request, null);
    }

    @ExceptionHandler(UnknownRoleException.class)
    public ResponseEntity<ErrorResponse> handleUnknownRole(UnknownRoleException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "UNKNOWN_ROLE", ex.getMessage(), request, null);
    }

    @ExceptionHandler(SelfDisableException.class)
    public ResponseEntity<ErrorResponse> handleSelfDisable(SelfDisableException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "SELF_DISABLE_NOT_ALLOWED", ex.getMessage(), request, null);
    }

    @ExceptionHandler(TimeClockConflictException.class)
    public ResponseEntity<ErrorResponse> handleTimeClockConflict(TimeClockConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidPolicyCategoryException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPolicyCategory(InvalidPolicyCategoryException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_CATEGORY", ex.getMessage(), request, null);
    }

    @ExceptionHandler(PolicyDocumentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePolicyDocumentNotFound(PolicyDocumentNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "POLICY_DOCUMENT_NOT_FOUND", ex.getMessage(), request, null);
    }

    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleConversationNotFound(ConversationNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND", ex.getMessage(), request, null);
    }

    @ExceptionHandler(ConversationAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleConversationAccessDenied(ConversationAccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "CONVERSATION_ACCESS_DENIED", ex.getMessage(), request, null);
    }

    @ExceptionHandler(PolicyDocumentStateException.class)
    public ResponseEntity<ErrorResponse> handlePolicyDocumentState(PolicyDocumentStateException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(DocumentUploadException.class)
    public ResponseEntity<ErrorResponse> handleDocumentUpload(DocumentUploadException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(DocumentProcessingException.class)
    public ResponseEntity<ErrorResponse> handleDocumentProcessing(DocumentProcessingException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(AiUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleAiUnavailable(AiUnavailableException ex, HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", ex.getMessage(), request, null);
    }

    @ExceptionHandler(DisabledAccountException.class)
    public ResponseEntity<ErrorResponse> handleDisabledAccount(DisabledAccountException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "ACCOUNT_DISABLED", ex.getMessage(), request, null);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleSpringDisabled(DisabledException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "ACCOUNT_DISABLED", "This account has been disabled.", request, null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password.", request, null);
    }

    // @PreAuthorize denials thrown inside a controller method are resolved by this
    // @RestControllerAdvice before they ever propagate out to the security filter
    // chain, so RestAccessDeniedHandler never sees them. Mirror its response here.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You do not have permission to perform this action.", request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            fieldErrors.put(field, error.getDefaultMessage());
        });
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", request, null);
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String errorCode, String message, HttpServletRequest request, Map<String, String> fieldErrors) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                errorCode,
                message,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}

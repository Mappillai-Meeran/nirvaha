package com.nirvaha.nirvaha_backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        Map<String, Object> body = createErrorBody(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateResource(DuplicateResourceException ex, HttpServletRequest request) {
        Map<String, Object> body = createErrorBody(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex, HttpServletRequest request) {
        String message = "Database constraint violation: A conflicting record already exists or data integrity rule was violated.";
        if (ex.getMessage() != null && ex.getMessage().contains("uq_incident_feedbacks_resolution")) {
            message = "Feedback has already been recorded for this incident resolution.";
        }
        Map<String, Object> body = createErrorBody(HttpStatus.CONFLICT, "Conflict", message, request);
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(org.springframework.security.core.AuthenticationException ex, HttpServletRequest request) {
        Map<String, Object> body = createErrorBody(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage() != null ? ex.getMessage() : "Authentication failed", request);
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex, HttpServletRequest request) {
        Map<String, Object> body = createErrorBody(HttpStatus.BAD_REQUEST, "Bad Request", "Malformed JSON request payload", request);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        Map<String, Object> body = createErrorBody(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> body = createErrorBody(HttpStatus.BAD_REQUEST, "Validation Failed", "Validation failed for one or more fields", request);
        body.put("errors", errors);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(org.springframework.web.multipart.MaxUploadSizeExceededException ex, HttpServletRequest request) {
        Map<String, Object> body = createErrorBody(HttpStatus.PAYLOAD_TOO_LARGE, "Payload Too Large", "File upload exceeds the maximum allowed size limit.", request);
        return new ResponseEntity<>(body, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(InvalidWorkflowException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidWorkflow(InvalidWorkflowException ex, HttpServletRequest request) {
        Map<String, Object> body = createErrorBody(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex, HttpServletRequest request) {
        String msg = ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : "Access Denied: You do not have sufficient privileges to access this resource";
        Map<String, Object> body = createErrorBody(HttpStatus.FORBIDDEN, "Forbidden", msg, request);
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex, HttpServletRequest request) {
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class).error("Unhandled exception at URI {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        Map<String, Object> body = createErrorBody(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected internal error occurred: " + ex.getMessage(), request);
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private Map<String, Object> createErrorBody(HttpStatus status, String errorLabel, String message, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", errorLabel);
        body.put("message", message);
        if (request != null && request.getRequestURI() != null) {
            body.put("path", request.getRequestURI());
        }
        return body;
    }
}

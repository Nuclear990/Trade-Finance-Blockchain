package com.tradeAnchor.backend.exceptionHandler;

import com.tradeAnchor.backend.exception.WalletCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void ignoreMissingStaticResources() {
        // deliberately empty
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDuplicateUser() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Username already taken");
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<String> handleNoSuchElement() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Resource not found");
    }

    @ExceptionHandler(WalletCreationException.class)
    public ResponseEntity<String> handleWalletFailure() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Wallet generation failed");
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<String> handleInvalidCredentials() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Invalid username or password");
    }

    @ExceptionHandler(com.tradeAnchor.backend.exception.ResourceNotFoundException.class)
    public ResponseEntity<String> handleNotFound(com.tradeAnchor.backend.exception.ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(com.tradeAnchor.backend.exception.ForbiddenException.class)
    public ResponseEntity<String> handleForbidden(com.tradeAnchor.backend.exception.ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler(com.tradeAnchor.backend.exception.ConflictException.class)
    public ResponseEntity<String> handleConflict(com.tradeAnchor.backend.exception.ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(err -> errors.put(
                        err.getField(),
                        err.getDefaultMessage()));

        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleInvalidEnum() {
        Map<String, String> error = new HashMap<>();
        error.put("userType", "Invalid userType");
        return ResponseEntity.badRequest().body(error);
    }

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Internal server error");
    }
}
